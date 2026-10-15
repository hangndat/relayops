package com.relayops.api.service;

import com.relayops.api.config.RelayOpsProperties;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeliveryQueue {

  private final StringRedisTemplate redis;
  private final RelayOpsProperties properties;

  public DeliveryQueue(StringRedisTemplate redis, RelayOpsProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  public void enqueue(UUID eventId) {
    redis.opsForList().rightPush(properties.getDelivery().getQueueKey(), eventId.toString());
  }

  public UUID dequeue() {
    String value = redis.opsForList().leftPop(properties.getDelivery().getQueueKey());
    if (value == null || value.isBlank()) {
      return null;
    }
    return UUID.fromString(value);
  }
}
