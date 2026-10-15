package com.relayops.api.web.dto;

import com.relayops.api.domain.DeliveryStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventResponse(
    UUID id,
    UUID destinationId,
    String idempotencyKey,
    DeliveryStatus status,
    int attemptCount,
    Instant nextAttemptAt,
    String lastError,
    Instant createdAt,
    Instant updatedAt,
    List<AttemptResponse> attempts) {

  public record AttemptResponse(
      UUID id,
      int attemptNumber,
      Instant startedAt,
      Instant finishedAt,
      Integer httpStatus,
      boolean success,
      String errorMessage) {}
}
