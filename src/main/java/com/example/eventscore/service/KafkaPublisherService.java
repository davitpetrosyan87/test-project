package com.example.eventscore.service;

import com.example.eventscore.dto.ScoreEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaPublisherService {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublisherService.class);

    private final KafkaTemplate<String, ScoreEvent> kafkaTemplate;
    private final String topic;

    public KafkaPublisherService(KafkaTemplate<String, ScoreEvent> kafkaTemplate,
                                 @Value("${app.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(ScoreEvent event) {
        kafkaTemplate.send(topic, event.eventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish score event for {}: {}",
                                event.eventId(), ex.getMessage(), ex);
                    } else {
                        log.info("Published score event for event {} to topic {} [partition={}, offset={}]",
                                event.eventId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
