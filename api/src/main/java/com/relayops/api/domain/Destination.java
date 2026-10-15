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
@Table(name = "destinations")
public class Destination {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "api_client_id", nullable = false)
  private ApiClient apiClient;

  @Column(nullable = false)
  private String name;

  @Column(name = "target_url", nullable = false)
  private String targetUrl;

  @Column(name = "signing_secret", nullable = false)
  private String signingSecret;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Destination() {}

  public Destination(
      UUID id,
      ApiClient apiClient,
      String name,
      String targetUrl,
      String signingSecret,
      boolean active,
      Instant createdAt) {
    this.id = id;
    this.apiClient = apiClient;
    this.name = name;
    this.targetUrl = targetUrl;
    this.signingSecret = signingSecret;
    this.active = active;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public String getName() {
    return name;
  }

  public String getTargetUrl() {
    return targetUrl;
  }

  public String getSigningSecret() {
    return signingSecret;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
