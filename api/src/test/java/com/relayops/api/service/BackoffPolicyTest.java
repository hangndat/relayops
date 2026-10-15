package com.relayops.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.relayops.api.config.RelayOpsProperties;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BackoffPolicyTest {

  @Test
  void exponentialBackoffIsBounded() {
    RelayOpsProperties properties = new RelayOpsProperties();
    properties.getDelivery().setInitialBackoffMs(500);
    properties.getDelivery().setMaxBackoffMs(2_000);
    properties.getDelivery().setMaxAttempts(3);
    BackoffPolicy policy = new BackoffPolicy(properties);

    Instant now = Instant.parse("2026-10-15T00:00:00Z");
    assertEquals(now.plusMillis(500), policy.nextAttemptAt(0, now));
    assertEquals(now.plusMillis(1_000), policy.nextAttemptAt(1, now));
    assertEquals(now.plusMillis(2_000), policy.nextAttemptAt(2, now));
    assertTrue(policy.canRetry(2));
    assertFalse(policy.canRetry(3));
  }
}
