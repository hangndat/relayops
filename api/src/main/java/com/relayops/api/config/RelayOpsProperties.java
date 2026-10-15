package com.relayops.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "relayops")
public class RelayOpsProperties {

  private String bootstrapApiKey = "dev-bootstrap-key-change-me";
  private final Delivery delivery = new Delivery();
  private final Worker worker = new Worker();

  public String getBootstrapApiKey() {
    return bootstrapApiKey;
  }

  public void setBootstrapApiKey(String bootstrapApiKey) {
    this.bootstrapApiKey = bootstrapApiKey;
  }

  public Delivery getDelivery() {
    return delivery;
  }

  public Worker getWorker() {
    return worker;
  }

  public static class Delivery {
    private String queueKey = "relayops:delivery:queue";
    private int maxAttempts = 5;
    private long initialBackoffMs = 500;
    private long maxBackoffMs = 30_000;
    private long httpTimeoutMs = 2_000;
    private long pollIntervalMs = 250;

    public String getQueueKey() {
      return queueKey;
    }

    public void setQueueKey(String queueKey) {
      this.queueKey = queueKey;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public long getInitialBackoffMs() {
      return initialBackoffMs;
    }

    public void setInitialBackoffMs(long initialBackoffMs) {
      this.initialBackoffMs = initialBackoffMs;
    }

    public long getMaxBackoffMs() {
      return maxBackoffMs;
    }

    public void setMaxBackoffMs(long maxBackoffMs) {
      this.maxBackoffMs = maxBackoffMs;
    }

    public long getHttpTimeoutMs() {
      return httpTimeoutMs;
    }

    public void setHttpTimeoutMs(long httpTimeoutMs) {
      this.httpTimeoutMs = httpTimeoutMs;
    }

    public long getPollIntervalMs() {
      return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
      this.pollIntervalMs = pollIntervalMs;
    }
  }

  public static class Worker {
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
