package com.relayops.api.web.dto;

import java.util.UUID;

public record BootstrapResponse(UUID clientId, String name, String apiKey) {}
