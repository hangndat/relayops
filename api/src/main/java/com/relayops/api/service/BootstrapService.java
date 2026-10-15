package com.relayops.api.service;

import com.relayops.api.config.RelayOpsProperties;
import com.relayops.api.domain.ApiClient;
import com.relayops.api.repo.ApiClientRepository;
import com.relayops.api.security.ApiKeyHasher;
import com.relayops.api.web.dto.BootstrapRequest;
import com.relayops.api.web.dto.BootstrapResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapService {

  private final ApiClientRepository apiClientRepository;
  private final ApiKeyHasher apiKeyHasher;
  private final RelayOpsProperties properties;
  private final SecureRandom secureRandom = new SecureRandom();

  public BootstrapService(
      ApiClientRepository apiClientRepository,
      ApiKeyHasher apiKeyHasher,
      RelayOpsProperties properties) {
    this.apiClientRepository = apiClientRepository;
    this.apiKeyHasher = apiKeyHasher;
    this.properties = properties;
  }

  @Transactional
  public BootstrapResponse bootstrap(BootstrapRequest request) {
    if (!properties.getBootstrapApiKey().equals(request.bootstrapKey())) {
      throw new SecurityException("Invalid bootstrap key");
    }
    byte[] raw = new byte[32];
    secureRandom.nextBytes(raw);
    String apiKey = "rok_" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    ApiClient client =
        new ApiClient(
            UUID.randomUUID(),
            request.name().trim(),
            apiKeyHasher.hash(apiKey),
            Instant.now());
    apiClientRepository.save(client);
    return new BootstrapResponse(client.getId(), client.getName(), apiKey);
  }
}
