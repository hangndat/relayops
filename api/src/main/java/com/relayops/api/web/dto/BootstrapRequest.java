package com.relayops.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BootstrapRequest(
    @NotBlank @Size(max = 120) String name, @NotBlank String bootstrapKey) {}
