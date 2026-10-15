package com.relayops.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "inbound_events")
public class InboundEvent {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "api_client_id", nullable = false)
  private ApiClient apiClient;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "destination_id", nullable = false)
  private Destination destination;

  @Column(name = "idempotency_key", nullable = false)
  private String idempotencyKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryStatus status;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "last_error")
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected InboundEvent() {}

  public InboundEvent(
      UUID id,
      ApiClient apiClient,
      Destination destination,
      String idempotencyKey,
      String payload,
      DeliveryStatus status,
      Instant createdAt) {
    this.id = id;
    this.apiClient = apiClient;
    this.destination = destination;
    this.idempotencyKey = idempotencyKey;
    this.payload = payload;
    this.status = status;
    this.attemptCount = 0;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public Destination getDestination() {
    return destination;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getPayload() {
    return payload;
  }

  public DeliveryStatus getStatus() {
    return status;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markQueued(Instant at) {
    this.status = DeliveryStatus.QUEUED;
    this.nextAttemptAt = at;
    this.updatedAt = at;
  }

  public void prepareReplay(Instant at) {
    // Keep attemptCount so delivery_attempts (event_id, attempt_number) stays unique.
    this.status = DeliveryStatus.QUEUED;
    this.nextAttemptAt = at;
    this.lastError = null;
    this.updatedAt = at;
  }

  public void markDelivering(Instant at) {
    this.status = DeliveryStatus.DELIVERING;
    this.updatedAt = at;
  }

  public void markSucceeded(Instant at) {
    this.status = DeliveryStatus.SUCCEEDED;
    this.nextAttemptAt = null;
    this.lastError = null;
    this.updatedAt = at;
  }

  public void markRetryable(String error, Instant nextAttempt, Instant at) {
    this.status = DeliveryStatus.FAILED_RETRYABLE;
    this.lastError = truncate(error, 1000);
    this.nextAttemptAt = nextAttempt;
    this.updatedAt = at;
  }

  public void markTerminal(String error, Instant at) {
    this.status = DeliveryStatus.FAILED_TERMINAL;
    this.lastError = truncate(error, 1000);
    this.nextAttemptAt = null;
    this.updatedAt = at;
  }

  public void incrementAttempt() {
    this.attemptCount += 1;
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    return value.length() <= max ? value : value.substring(0, max);
  }
}
