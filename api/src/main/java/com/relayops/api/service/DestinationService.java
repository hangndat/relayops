package com.relayops.api.service;

import com.relayops.api.domain.ApiClient;
import com.relayops.api.domain.Destination;
import com.relayops.api.repo.DestinationRepository;
import com.relayops.api.security.AuthContext;
import com.relayops.api.web.dto.CreateDestinationRequest;
import com.relayops.api.web.dto.DestinationResponse;
import com.relayops.api.web.dto.RotateSecretRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DestinationService {

  private final DestinationRepository destinationRepository;

  public DestinationService(DestinationRepository destinationRepository) {
    this.destinationRepository = destinationRepository;
  }

  @Transactional
  public DestinationResponse create(CreateDestinationRequest request) {
    ApiClient client = AuthContext.require();
    Destination destination =
        new Destination(
            UUID.randomUUID(),
            client,
            request.name().trim(),
            request.targetUrl().trim(),
            request.signingSecret(),
            true,
            Instant.now());
    destinationRepository.save(destination);
    return toResponse(destination);
  }

  @Transactional(readOnly = true)
  public List<DestinationResponse> list() {
    ApiClient client = AuthContext.require();
    return destinationRepository.findByApiClientIdOrderByCreatedAtDesc(client.getId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public DestinationResponse rotateSecret(UUID destinationId, RotateSecretRequest request) {
    ApiClient client = AuthContext.require();
    Destination destination =
        destinationRepository
            .findByIdAndApiClientId(destinationId, client.getId())
            .orElseThrow(() -> new IllegalArgumentException("Destination not found"));
    destination.rotateSigningSecret(request.signingSecret());
    return toResponse(destination);
  }

  private DestinationResponse toResponse(Destination destination) {
    return new DestinationResponse(
        destination.getId(),
        destination.getName(),
        destination.getTargetUrl(),
        destination.isActive(),
        destination.getCreatedAt());
  }
}
