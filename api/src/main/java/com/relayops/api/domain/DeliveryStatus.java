package com.relayops.api.domain;

public enum DeliveryStatus {
  RECEIVED,
  QUEUED,
  DELIVERING,
  SUCCEEDED,
  FAILED_RETRYABLE,
  FAILED_TERMINAL
}
