package com.relayops.api.web;

import com.relayops.api.service.EventOperationsService;
import com.relayops.api.web.dto.EventResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dlq")
public class DlqController {

  private final EventOperationsService eventOperationsService;

  public DlqController(EventOperationsService eventOperationsService) {
    this.eventOperationsService = eventOperationsService;
  }

  @GetMapping
  public List<EventResponse> list(@RequestParam(defaultValue = "50") int limit) {
    return eventOperationsService.dlq(limit);
  }
}
