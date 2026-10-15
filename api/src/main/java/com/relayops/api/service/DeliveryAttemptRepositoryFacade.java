package com.relayops.api.service;

import com.relayops.api.domain.DeliveryAttempt;
import com.relayops.api.repo.DeliveryAttemptRepository;
import com.relayops.api.web.dto.EventResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DeliveryAttemptRepositoryFacade {

  private final DeliveryAttemptRepository deliveryAttemptRepository;

  public DeliveryAttemptRepositoryFacade(DeliveryAttemptRepository deliveryAttemptRepository) {
    this.deliveryAttemptRepository = deliveryAttemptRepository;
  }

  public List<EventResponse.AttemptResponse> list(UUID eventId) {
    return deliveryAttemptRepository.findByEventIdOrderByAttemptNumberAsc(eventId).stream()
        .map(this::toResponse)
        .toList();
  }

  public DeliveryAttempt save(DeliveryAttempt attempt) {
    return deliveryAttemptRepository.save(attempt);
  }

  private EventResponse.AttemptResponse toResponse(DeliveryAttempt attempt) {
    return new EventResponse.AttemptResponse(
        attempt.getId(),
        attempt.getAttemptNumber(),
        attempt.getStartedAt(),
        attempt.getFinishedAt(),
        attempt.getHttpStatus(),
        attempt.isSuccess(),
        attempt.getErrorMessage());
  }
}
