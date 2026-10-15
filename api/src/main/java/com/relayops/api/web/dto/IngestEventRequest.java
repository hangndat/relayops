package com.relayops.api.web.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IngestEventRequest(
    @NotNull UUID destinationId, @NotBlank String idempotencyKey, @NotNull JsonNode payload) {}
