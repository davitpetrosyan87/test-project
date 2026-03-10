package com.example.eventscore.dto;

import java.time.Instant;

public record ScoreEvent(String eventId, String score, Instant timestamp) {
}
