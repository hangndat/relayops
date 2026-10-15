package com.relayops.api.service;

import com.relayops.api.domain.ApiClient;
import com.relayops.api.domain.DeliveryStatus;
import com.relayops.api.domain.InboundEvent;
import com.relayops.api.repo.InboundEventRepository;
import com.relayops.api.security.AuthContext;
import com.relayops.api.web.dto.EventResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class EventOperationsService {

  private final InboundEventRepository inboundEventRepository;
  private final DeliveryAttemptRepositoryFacade attempts;
  private final DeliveryQueue deliveryQueue;

  public EventOperationsService(
      InboundEventRepository inboundEventRepository,
      DeliveryAttemptRepositoryFacade attempts,
      DeliveryQueue deliveryQueue) {
    this.inboundEventRepository = inboundEventRepository;
    this.attempts = attempts;
    this.deliveryQueue = deliveryQueue;
  }

  @Transactional(readOnly = true)
  public List<EventResponse> list(DeliveryStatus status, int limit) {
    ApiClient client = AuthContext.require();
    int pageSize = Math.min(Math.max(limit, 1), 100);
    List<InboundEvent> events =
        status == null
            ? inboundEventRepository.findByApiClientIdOrderByCreatedAtDesc(
                client.getId(), PageRequest.of(0, pageSize))
            : inboundEventRepository.findByApiClientIdAndStatusOrderByCreatedAtDesc(
                client.getId(), status, PageRequest.of(0, pageSize));
    return events.stream()
        .map(event -> EventIngestionService.toResponse(event, attempts.list(event.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<EventResponse> dlq(int limit) {
    return list(DeliveryStatus.FAILED_TERMINAL, limit);
  }

  @Transactional
  public EventResponse replay(UUID eventId) {
    ApiClient client = AuthContext.require();
    InboundEvent event =
        inboundEventRepository
            .findByIdAndApiClientId(eventId, client.getId())
            .orElseThrow(() -> new IllegalArgumentException("Event not found"));

    if (event.getStatus() == DeliveryStatus.SUCCEEDED) {
      throw new IllegalArgumentException("Succeeded events cannot be replayed");
    }
    if (event.getStatus() == DeliveryStatus.DELIVERING) {
      throw new IllegalArgumentException("Event is already delivering");
    }

    Instant now = Instant.now();
    event.prepareReplay(now);
    inboundEventRepository.save(event);
    enqueueAfterCommit(event.getId());
    return EventIngestionService.toResponse(event, attempts.list(event.getId()));
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
}
