package com.relayops.api.repo;

import com.relayops.api.domain.ApiClient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {
  Optional<ApiClient> findByApiKeyHash(String apiKeyHash);
}
