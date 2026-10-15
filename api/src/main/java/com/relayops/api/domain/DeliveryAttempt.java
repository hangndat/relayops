package com.relayops.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttempt {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private InboundEvent event;

  @Column(name = "attempt_number", nullable = false)
  private int attemptNumber;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "http_status")
  private Integer httpStatus;

  @Column(nullable = false)
  private boolean success;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "response_body_excerpt")
  private String responseBodyExcerpt;

  protected DeliveryAttempt() {}

  public DeliveryAttempt(UUID id, InboundEvent event, int attemptNumber, Instant startedAt) {
    this.id = id;
    this.event = event;
    this.attemptNumber = attemptNumber;
    this.startedAt = startedAt;
    this.success = false;
  }

  public void complete(
      Instant finishedAt,
      Integer httpStatus,
      boolean success,
      String errorMessage,
      String responseBodyExcerpt) {
    this.finishedAt = finishedAt;
    this.httpStatus = httpStatus;
    this.success = success;
    this.errorMessage = truncate(errorMessage, 1000);
    this.responseBodyExcerpt = truncate(responseBodyExcerpt, 500);
  }

  public UUID getId() {
    return id;
  }

  public InboundEvent getEvent() {
    return event;
  }

  public int getAttemptNumber() {
    return attemptNumber;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public Integer getHttpStatus() {
    return httpStatus;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getResponseBodyExcerpt() {
    return responseBodyExcerpt;
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    return value.length() <= max ? value : value.substring(0, max);
  }
}
