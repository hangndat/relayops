package com.relayops.api.web.dto;

import java.time.Instant;
import java.util.UUID;

public record DestinationResponse(
    UUID id, String name, String targetUrl, boolean active, Instant createdAt) {}
