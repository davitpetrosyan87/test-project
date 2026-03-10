package com.example.eventscore.service;

import com.example.eventscore.dto.ExternalScoreResponse;
import com.example.eventscore.dto.ScoreEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
public class ScorePollingService {

    private static final Logger log = LoggerFactory.getLogger(ScorePollingService.class);

    private final RestClient externalApiClient;
    private final KafkaPublisherService kafkaPublisherService;

    public ScorePollingService(RestClient externalApiClient, KafkaPublisherService kafkaPublisherService) {
        this.externalApiClient = externalApiClient;
        this.kafkaPublisherService = kafkaPublisherService;
    }

    public void pollAndPublish(String eventId) {
        try {
            log.debug("Polling score for event: {}", eventId);

            ExternalScoreResponse response = externalApiClient
                    .get()
                    .uri("/events/{eventId}/score", eventId)
                    .retrieve()
                    .body(ExternalScoreResponse.class);

            if (response == null) {
                log.warn("Received null response from external API for event: {}", eventId);
                return;
            }

            ScoreEvent scoreEvent = new ScoreEvent(
                    response.eventId(),
                    response.currentScore(),
                    Instant.now()
            );

            kafkaPublisherService.publish(scoreEvent);
            log.debug("Successfully polled and published score for event: {}", eventId);

        } catch (Exception e) {
            log.error("Error polling score for event {}: {}", eventId, e.getMessage(), e);
            // Do NOT rethrow — a thrown exception from a scheduled task would cancel the schedule
        }
    }
}
