package com.relayops.api.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.relayops.api.domain.ApiClient;
import com.relayops.api.domain.DeliveryStatus;
import com.relayops.api.domain.Destination;
import com.relayops.api.domain.InboundEvent;
import com.relayops.api.repo.DestinationRepository;
import com.relayops.api.repo.InboundEventRepository;
import com.relayops.api.security.AuthContext;
import com.relayops.api.web.dto.EventResponse;
import com.relayops.api.web.dto.IngestEventRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class EventIngestionService {

  private final DestinationRepository destinationRepository;
  private final InboundEventRepository inboundEventRepository;
  private final DeliveryAttemptRepositoryFacade attempts;
  private final DeliveryQueue deliveryQueue;
  private final HmacSigner hmacSigner;
  private final ObjectMapper objectMapper;

  public EventIngestionService(
      DestinationRepository destinationRepository,
      InboundEventRepository inboundEventRepository,
      DeliveryAttemptRepositoryFacade attempts,
      DeliveryQueue deliveryQueue,
      HmacSigner hmacSigner,
      ObjectMapper objectMapper) {
    this.destinationRepository = destinationRepository;
    this.inboundEventRepository = inboundEventRepository;
    this.attempts = attempts;
    this.deliveryQueue = deliveryQueue;
    this.hmacSigner = hmacSigner;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public EventResponse ingest(IngestEventRequest request, String providedSignature) {
    ApiClient client = AuthContext.require();
    Destination destination =
        destinationRepository
            .findByIdAndApiClientId(request.destinationId(), client.getId())
            .orElseThrow(() -> new IllegalArgumentException("Destination not found"));
    if (!destination.isActive()) {
      throw new IllegalArgumentException("Destination is inactive");
    }

    final String payloadJson;
    try {
      payloadJson = objectMapper.writeValueAsString(request.payload());
    } catch (JacksonException ex) {
      throw new IllegalArgumentException("Invalid payload JSON", ex);
    }

    if (!hmacSigner.matches(destination.getSigningSecret(), payloadJson, providedSignature)) {
      throw new SecurityException("Invalid webhook signature");
    }

    return inboundEventRepository
        .findByApiClientIdAndIdempotencyKey(client.getId(), request.idempotencyKey())
        .map(existing -> toResponse(existing, attempts.list(existing.getId())))
        .orElseGet(() -> createNew(client, destination, request.idempotencyKey(), payloadJson));
  }

  @Transactional(readOnly = true)
  public EventResponse get(UUID eventId) {
    ApiClient client = AuthContext.require();
    InboundEvent event =
        inboundEventRepository
            .findByIdAndApiClientId(eventId, client.getId())
            .orElseThrow(() -> new IllegalArgumentException("Event not found"));
    return toResponse(event, attempts.list(event.getId()));
  }

  private EventResponse createNew(
      ApiClient client, Destination destination, String idempotencyKey, String payloadJson) {
    Instant now = Instant.now();
    InboundEvent event =
        new InboundEvent(
            UUID.randomUUID(),
            client,
            destination,
            idempotencyKey,
            payloadJson,
            DeliveryStatus.RECEIVED,
            now);
    event.markQueued(now);
    inboundEventRepository.save(event);
    enqueueAfterCommit(event.getId());
    return toResponse(event, List.of());
  }

  private void enqueueAfterCommit(UUID eventId) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      deliveryQueue.enqueue(eventId);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            deliveryQueue.enqueue(eventId);
          }
        });
  }

  static EventResponse toResponse(
      InboundEvent event, List<EventResponse.AttemptResponse> attemptResponses) {
    return new EventResponse(
        event.getId(),
        event.getDestination().getId(),
        event.getIdempotencyKey(),
        event.getStatus(),
        event.getAttemptCount(),
        event.getNextAttemptAt(),
        event.getLastError(),
        event.getCreatedAt(),
        event.getUpdatedAt(),
        attemptResponses);
  }
}
