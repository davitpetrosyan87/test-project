package com.example.eventscore.service;

import com.example.eventscore.dto.ExternalScoreResponse;
import com.example.eventscore.dto.ScoreEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScorePollingServiceTest {

    @Mock
    private RestClient externalApiClient;

    @Mock
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private KafkaPublisherService kafkaPublisherService;

    @InjectMocks
    private ScorePollingService scorePollingService;

    @SuppressWarnings("unchecked")
    @Test
    void pollAndPublish_success_publishesToKafka() {
        ExternalScoreResponse apiResponse = new ExternalScoreResponse("event-1", "2:1");

        when(externalApiClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/events/{eventId}/score"), eq("event-1")))
                .thenReturn((RestClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ExternalScoreResponse.class)).thenReturn(apiResponse);

        scorePollingService.pollAndPublish("event-1");

        ArgumentCaptor<ScoreEvent> captor = ArgumentCaptor.forClass(ScoreEvent.class);
        verify(kafkaPublisherService).publish(captor.capture());

        ScoreEvent published = captor.getValue();
        assertThat(published.eventId()).isEqualTo("event-1");
        assertThat(published.score()).isEqualTo("2:1");
        assertThat(published.timestamp()).isNotNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void pollAndPublish_nullResponse_doesNotPublish() {
        when(externalApiClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/events/{eventId}/score"), eq("event-1")))
                .thenReturn((RestClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ExternalScoreResponse.class)).thenReturn(null);

        scorePollingService.pollAndPublish("event-1");

        verifyNoInteractions(kafkaPublisherService);
    }

    @SuppressWarnings("unchecked")
    @Test
    void pollAndPublish_apiError_doesNotThrow() {
        when(externalApiClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/events/{eventId}/score"), eq("event-1")))
                .thenReturn((RestClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RestClientException("Connection refused"));

        // Should NOT throw — errors are caught and logged
        scorePollingService.pollAndPublish("event-1");

        verifyNoInteractions(kafkaPublisherService);
    }
}
