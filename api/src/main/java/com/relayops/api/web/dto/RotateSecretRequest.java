package com.relayops.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RotateSecretRequest(@NotBlank @Size(min = 16, max = 256) String signingSecret) {}
