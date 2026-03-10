package com.example.eventscore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSchedulerServiceTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ScorePollingService scorePollingService;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private EventSchedulerService service;

    @BeforeEach
    void setUp() {
        service = new EventSchedulerService(taskScheduler, scorePollingService, 10);
    }

    @Test
    void updateStatus_live_schedulesPolling() {
        doReturn(scheduledFuture)
                .when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofSeconds(10)));

        service.updateStatus("event-1", "live");

        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofSeconds(10)));
        assertThat(service.getLiveEvents()).contains("event-1");
        assertThat(service.getActiveTaskCount()).isEqualTo(1);
    }

    @Test
    void updateStatus_liveTwice_schedulesOnlyOnce() {
        doReturn(scheduledFuture)
                .when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofSeconds(10)));

        service.updateStatus("event-1", "live");
        service.updateStatus("event-1", "live");

        verify(taskScheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofSeconds(10)));
        assertThat(service.getActiveTaskCount()).isEqualTo(1);
    }

    @Test
    void updateStatus_notLive_cancelsPolling() {
        doReturn(scheduledFuture)
                .when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofSeconds(10)));

        service.updateStatus("event-1", "live");
        service.updateStatus("event-1", "not live");

        verify(scheduledFuture).cancel(false);
        assertThat(service.getLiveEvents()).doesNotContain("event-1");
        assertThat(service.getActiveTaskCount()).isEqualTo(0);
    }

    @Test
    void updateStatus_notLiveWithoutLive_isNoOp() {
        service.updateStatus("unknown-event", "not live");

        verifyNoInteractions(taskScheduler);
        assertThat(service.getActiveTaskCount()).isEqualTo(0);
    }

    @Test
    void updateStatus_multipleEvents_trackedIndependently() {
        doReturn(scheduledFuture)
                .when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofSeconds(10)));

        service.updateStatus("event-1", "live");
        service.updateStatus("event-2", "live");

        assertThat(service.getLiveEvents()).containsExactlyInAnyOrder("event-1", "event-2");
        assertThat(service.getActiveTaskCount()).isEqualTo(2);
    }
}
