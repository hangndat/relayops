package com.relayops.api.service;

import com.relayops.api.config.RelayOpsProperties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryWorker {

  private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);

  private final DeliveryQueue deliveryQueue;
  private final DeliveryProcessor deliveryProcessor;
  private final RelayOpsProperties properties;

  public DeliveryWorker(
      DeliveryQueue deliveryQueue,
      DeliveryProcessor deliveryProcessor,
      RelayOpsProperties properties) {
    this.deliveryQueue = deliveryQueue;
    this.deliveryProcessor = deliveryProcessor;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString = "${relayops.delivery.poll-interval-ms:250}")
  public void poll() {
    if (!properties.getWorker().isEnabled()) {
      return;
    }
    UUID eventId = deliveryQueue.dequeue();
    if (eventId == null) {
      return;
    }
    try {
      deliveryProcessor.process(eventId);
    } catch (Exception ex) {
      log.error("Delivery worker failed for event {}", eventId, ex);
      deliveryQueue.enqueue(eventId);
    }
  }
}
