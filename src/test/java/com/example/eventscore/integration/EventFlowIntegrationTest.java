package com.example.eventscore.integration;

import com.example.eventscore.dto.ScoreEvent;
import com.example.eventscore.service.EventSchedulerService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8089",
                "app.polling.interval-seconds=1",
                "app.external-api.base-url=http://localhost:8089"
        }
)
@EmbeddedKafka(partitions = 1, topics = "event-scores",
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@ActiveProfiles("mock-api")
class EventFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private EventSchedulerService eventSchedulerService;

    private KafkaMessageListenerContainer<String, ScoreEvent> container;
    private BlockingQueue<ConsumerRecord<String, ScoreEvent>> records;

    @BeforeEach
    void setUp() {
        records = new LinkedBlockingQueue<>();

        JsonDeserializer<ScoreEvent> deserializer = new JsonDeserializer<>(ScoreEvent.class);
        deserializer.addTrustedPackages("*");

        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-group",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );

        DefaultKafkaConsumerFactory<String, ScoreEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), deserializer);

        ContainerProperties containerProps = new ContainerProperties("event-scores");
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProps);
        container.setupMessageListener((MessageListener<String, ScoreEvent>) records::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
        eventSchedulerService.getLiveEvents().forEach(id ->
                eventSchedulerService.updateStatus(id, "not live"));
    }

    @Test
    void fullFlow_setLive_pollsAndPublishes() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/events/status",
                Map.of("eventId", "integration-test-1", "status", "live"),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ConsumerRecord<String, ScoreEvent> record = records.poll(15, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo("integration-test-1");
        assertThat(record.value().eventId()).isEqualTo("integration-test-1");
        assertThat(record.value().score()).isNotNull();
        assertThat(record.value().timestamp()).isNotNull();
    }

    @Test
    void fullFlow_setNotLive_stopsPolling() throws Exception {
        restTemplate.postForEntity(
                "/events/status",
                Map.of("eventId", "integration-test-2", "status", "live"),
                String.class
        );

        ConsumerRecord<String, ScoreEvent> record = records.poll(15, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        restTemplate.postForEntity(
                "/events/status",
                Map.of("eventId", "integration-test-2", "status", "not live"),
                String.class
        );

        assertThat(eventSchedulerService.getLiveEvents()).doesNotContain("integration-test-2");
    }
}
