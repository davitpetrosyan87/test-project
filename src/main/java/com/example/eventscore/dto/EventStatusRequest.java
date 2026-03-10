package com.example.eventscore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EventStatusRequest(
        @NotBlank(message = "eventId is required")
        String eventId,

        @NotBlank(message = "status is required")
        @Pattern(regexp = "live|not live", message = "status must be 'live' or 'not live'")
        String status
) {
}
