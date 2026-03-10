package com.example.eventscore.controller;

import com.example.eventscore.dto.EventStatusRequest;
import com.example.eventscore.service.EventSchedulerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final EventSchedulerService eventSchedulerService;

    public EventController(EventSchedulerService eventSchedulerService) {
        this.eventSchedulerService = eventSchedulerService;
    }

    @PostMapping("/status")
    public ResponseEntity<Map<String, String>> updateStatus(@Valid @RequestBody EventStatusRequest request) {
        log.info("Received status update: eventId={}, status={}", request.eventId(), request.status());
        eventSchedulerService.updateStatus(request.eventId(), request.status());
        return ResponseEntity.ok(Map.of(
                "eventId", request.eventId(),
                "status", request.status(),
                "message", "Event status updated successfully"
        ));
    }
}
