package com.relayops.api.service;

import com.relayops.api.config.RelayOpsProperties;
import com.relayops.api.domain.DeliveryAttempt;
import com.relayops.api.domain.DeliveryStatus;
import com.relayops.api.domain.InboundEvent;
import com.relayops.api.repo.InboundEventRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryProcessor {

  private static final Logger log = LoggerFactory.getLogger(DeliveryProcessor.class);

  private final DeliveryQueue deliveryQueue;
  private final InboundEventRepository inboundEventRepository;
  private final DeliveryAttemptRepositoryFacade attempts;
  private final HmacSigner hmacSigner;
  private final BackoffPolicy backoffPolicy;
  private final HttpClient httpClient;
  private final RelayOpsProperties properties;

  public DeliveryProcessor(
      DeliveryQueue deliveryQueue,
      InboundEventRepository inboundEventRepository,
      DeliveryAttemptRepositoryFacade attempts,
      HmacSigner hmacSigner,
      BackoffPolicy backoffPolicy,
      HttpClient httpClient,
      RelayOpsProperties properties) {
    this.deliveryQueue = deliveryQueue;
    this.inboundEventRepository = inboundEventRepository;
    this.attempts = attempts;
    this.hmacSigner = hmacSigner;
    this.backoffPolicy = backoffPolicy;
    this.httpClient = httpClient;
    this.properties = properties;
  }

  @Transactional
  public void process(UUID eventId) {
    InboundEvent event = inboundEventRepository.findById(eventId).orElse(null);
    if (event == null) {
      log.warn("Skipping missing event {}", eventId);
      return;
    }

    if (event.getStatus() == DeliveryStatus.SUCCEEDED
        || event.getStatus() == DeliveryStatus.FAILED_TERMINAL) {
      return;
    }

    Instant now = Instant.now();
    if (event.getNextAttemptAt() != null && event.getNextAttemptAt().isAfter(now)) {
      deliveryQueue.enqueue(eventId);
      return;
    }

    event.incrementAttempt();
    event.markDelivering(now);
    inboundEventRepository.saveAndFlush(event);

    DeliveryAttempt attempt =
        new DeliveryAttempt(UUID.randomUUID(), event, event.getAttemptCount(), now);
    String signature =
        hmacSigner.signHex(event.getDestination().getSigningSecret(), event.getPayload());

    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(event.getDestination().getTargetUrl()))
              .timeout(Duration.ofMillis(properties.getDelivery().getHttpTimeoutMs()))
              .header("Content-Type", "application/json")
              .header("X-RelayOps-Signature", "sha256=" + signature)
              .header("X-RelayOps-Event-Id", event.getId().toString())
              .header("X-RelayOps-Delivery-Attempt", String.valueOf(event.getAttemptCount()))
              .POST(HttpRequest.BodyPublishers.ofString(event.getPayload()))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      Instant finished = Instant.now();
      boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
      attempt.complete(
          finished,
          response.statusCode(),
          success,
          success ? null : "HTTP " + response.statusCode(),
          response.body());
      attempts.save(attempt);

      if (success) {
        event.markSucceeded(finished);
        log.info(
            "event_delivered eventId={} attempt={} status={}",
            event.getId(),
            event.getAttemptCount(),
            response.statusCode());
      } else {
        handleFailure(event, "HTTP " + response.statusCode(), finished);
      }
    } catch (Exception ex) {
      Instant finished = Instant.now();
      attempt.complete(finished, null, false, ex.getMessage(), null);
      attempts.save(attempt);
      handleFailure(event, ex.getMessage(), finished);
    }

    inboundEventRepository.save(event);
  }

  private void handleFailure(InboundEvent event, String error, Instant now) {
    if (backoffPolicy.canRetry(event.getAttemptCount())) {
      Instant next = backoffPolicy.nextAttemptAt(event.getAttemptCount(), now);
      event.markRetryable(error, next, now);
      deliveryQueue.enqueue(event.getId());
      log.warn(
          "event_retry_scheduled eventId={} attempt={} nextAttemptAt={} error={}",
          event.getId(),
          event.getAttemptCount(),
          next,
          error);
    } else {
      event.markTerminal(error, now);
      log.error(
          "event_terminal_failure eventId={} attempt={} error={}",
          event.getId(),
          event.getAttemptCount(),
          error);
    }
  }
}
