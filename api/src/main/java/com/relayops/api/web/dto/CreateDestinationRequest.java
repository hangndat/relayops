package com.relayops.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDestinationRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank
        @Pattern(regexp = "^https?://.+", message = "targetUrl must be http(s)")
        String targetUrl,
    @NotBlank @Size(min = 16, max = 256) String signingSecret) {}
