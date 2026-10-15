package com.relayops.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_clients")
public class ApiClient {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "api_key_hash", nullable = false, unique = true)
  private String apiKeyHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ApiClient() {}

  public ApiClient(UUID id, String name, String apiKeyHash, Instant createdAt) {
    this.id = id;
    this.name = name;
    this.apiKeyHash = apiKeyHash;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getApiKeyHash() {
    return apiKeyHash;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
