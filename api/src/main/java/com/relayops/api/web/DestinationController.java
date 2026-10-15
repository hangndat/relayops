package com.relayops.api.web;

import com.relayops.api.service.DestinationService;
import com.relayops.api.web.dto.CreateDestinationRequest;
import com.relayops.api.web.dto.DestinationResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/destinations")
public class DestinationController {

  private final DestinationService destinationService;

  public DestinationController(DestinationService destinationService) {
    this.destinationService = destinationService;
  }

  @PostMapping
  public DestinationResponse create(@Valid @RequestBody CreateDestinationRequest request) {
    return destinationService.create(request);
  }

  @GetMapping
  public List<DestinationResponse> list() {
    return destinationService.list();
  }
}
