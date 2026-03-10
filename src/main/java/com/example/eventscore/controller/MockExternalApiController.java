package com.example.eventscore.controller;

import com.example.eventscore.dto.ExternalScoreResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock implementation of the external score API.
 * Active only when the "mock-api" profile is enabled.
 * In production, the real external API URL would be configured instead.
 */
@RestController
@Profile("mock-api")
public class MockExternalApiController {

    private static final Logger log = LoggerFactory.getLogger(MockExternalApiController.class);

    private final Map<String, int[]> scores = new ConcurrentHashMap<>();

    @GetMapping("/events/{eventId}/score")
    public ExternalScoreResponse getScore(@PathVariable String eventId) {
        int[] score = scores.computeIfAbsent(eventId, id -> new int[]{0, 0});

        // Randomly increment one team's score ~20% of the time
        if (ThreadLocalRandom.current().nextInt(5) == 0) {
            if (ThreadLocalRandom.current().nextBoolean()) {
                score[0]++;
            } else {
                score[1]++;
            }
        }

        String currentScore = score[0] + ":" + score[1];
        log.debug("Mock API returning score for event {}: {}", eventId, currentScore);
        return new ExternalScoreResponse(eventId, currentScore);
    }
}
