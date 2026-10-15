package com.relayops.api.web;

import com.relayops.api.domain.DeliveryStatus;
import com.relayops.api.service.EventIngestionService;
import com.relayops.api.service.EventOperationsService;
import com.relayops.api.web.dto.EventResponse;
import com.relayops.api.web.dto.IngestEventRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/events")
public class EventController {

  private final EventIngestionService eventIngestionService;
  private final EventOperationsService eventOperationsService;

  public EventController(
      EventIngestionService eventIngestionService, EventOperationsService eventOperationsService) {
    this.eventIngestionService = eventIngestionService;
    this.eventOperationsService = eventOperationsService;
  }

  @PostMapping
  public EventResponse ingest(
      @Valid @RequestBody IngestEventRequest request,
      @RequestHeader(value = "X-RelayOps-Signature", required = false) String signature) {
    return eventIngestionService.ingest(request, signature);
  }

  @GetMapping
  public List<EventResponse> list(
      @RequestParam(required = false) DeliveryStatus status,
      @RequestParam(defaultValue = "50") int limit) {
    return eventOperationsService.list(status, limit);
  }

  @GetMapping("/{eventId}")
  public EventResponse get(@PathVariable UUID eventId) {
    return eventIngestionService.get(eventId);
  }

  @PostMapping("/{eventId}/replay")
  public EventResponse replay(@PathVariable UUID eventId) {
    return eventOperationsService.replay(eventId);
  }
}
