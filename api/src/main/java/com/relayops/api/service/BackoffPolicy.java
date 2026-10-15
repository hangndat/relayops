package com.relayops.api.service;

import com.relayops.api.config.RelayOpsProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class BackoffPolicy {

  private final RelayOpsProperties properties;

  public BackoffPolicy(RelayOpsProperties properties) {
    this.properties = properties;
  }

  public Instant nextAttemptAt(int attemptCount, Instant now) {
    long base = properties.getDelivery().getInitialBackoffMs();
    long max = properties.getDelivery().getMaxBackoffMs();
    long delay = Math.min(max, base * (1L << Math.min(attemptCount, 10)));
    return now.plus(Duration.ofMillis(delay));
  }

  public boolean canRetry(int attemptCount) {
    return attemptCount < properties.getDelivery().getMaxAttempts();
  }
}
