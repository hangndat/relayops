package com.relayops.api.web;

import com.relayops.api.service.EventIngestionService;
import com.relayops.api.web.dto.EventResponse;
import com.relayops.api.web.dto.IngestEventRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/events")
public class EventController {

  private final EventIngestionService eventIngestionService;

  public EventController(EventIngestionService eventIngestionService) {
    this.eventIngestionService = eventIngestionService;
  }

  @PostMapping
  public EventResponse ingest(
      @Valid @RequestBody IngestEventRequest request,
      @RequestHeader(value = "X-RelayOps-Signature", required = false) String signature) {
    return eventIngestionService.ingest(request, signature);
  }

  @GetMapping("/{eventId}")
  public EventResponse get(@PathVariable UUID eventId) {
    return eventIngestionService.get(eventId);
  }
}
