package com.example.eventscore.service;

import com.example.eventscore.dto.ScoreEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaPublisherServiceTest {

    @Mock
    private KafkaTemplate<String, ScoreEvent> kafkaTemplate;

    @Test
    void publish_sendsToCorrectTopicWithEventIdAsKey() {
        KafkaPublisherService service = new KafkaPublisherService(kafkaTemplate, "event-scores");
        ScoreEvent event = new ScoreEvent("event-1", "3:2", Instant.now());

        when(kafkaTemplate.send(eq("event-scores"), eq("event-1"), eq(event)))
                .thenReturn(new CompletableFuture<>());

        service.publish(event);

        verify(kafkaTemplate).send("event-scores", "event-1", event);
    }

    @Test
    void publish_handlesFailureGracefully() {
        KafkaPublisherService service = new KafkaPublisherService(kafkaTemplate, "event-scores");
        ScoreEvent event = new ScoreEvent("event-1", "1:0", Instant.now());

        CompletableFuture<SendResult<String, ScoreEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Broker unavailable"));

        when(kafkaTemplate.send(eq("event-scores"), eq("event-1"), eq(event)))
                .thenReturn(failedFuture);

        // Should not throw
        service.publish(event);
    }
}
