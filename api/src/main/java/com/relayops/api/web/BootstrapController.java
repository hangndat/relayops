package com.relayops.api.web;

import com.relayops.api.service.BootstrapService;
import com.relayops.api.web.dto.BootstrapRequest;
import com.relayops.api.web.dto.BootstrapResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/bootstrap")
public class BootstrapController {

  private final BootstrapService bootstrapService;

  public BootstrapController(BootstrapService bootstrapService) {
    this.bootstrapService = bootstrapService;
  }

  @PostMapping
  public BootstrapResponse bootstrap(@Valid @RequestBody BootstrapRequest request) {
    return bootstrapService.bootstrap(request);
  }
}
