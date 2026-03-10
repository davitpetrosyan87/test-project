package com.example.eventscore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class EventSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(EventSchedulerService.class);

    private final TaskScheduler taskScheduler;
    private final ScorePollingService scorePollingService;
    private final long pollingIntervalSeconds;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Set<String> liveEvents = ConcurrentHashMap.newKeySet();

    public EventSchedulerService(TaskScheduler taskScheduler,
                                 ScorePollingService scorePollingService,
                                 @Value("${app.polling.interval-seconds:10}") long pollingIntervalSeconds) {
        this.taskScheduler = taskScheduler;
        this.scorePollingService = scorePollingService;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    public void updateStatus(String eventId, String status) {
        if ("live".equals(status)) {
            startPolling(eventId);
        } else {
            stopPolling(eventId);
        }
    }

    private void startPolling(String eventId) {
        // Use computeIfAbsent for atomicity — avoids duplicate scheduling
        scheduledTasks.computeIfAbsent(eventId, id -> {
            log.info("Starting score polling for event: {}", id);
            liveEvents.add(id);
            return taskScheduler.scheduleAtFixedRate(
                    () -> scorePollingService.pollAndPublish(id),
                    Duration.ofSeconds(pollingIntervalSeconds)
            );
        });
    }

    private void stopPolling(String eventId) {
        ScheduledFuture<?> future = scheduledTasks.remove(eventId);
        if (future != null) {
            future.cancel(false);
            liveEvents.remove(eventId);
            log.info("Stopped score polling for event: {}", eventId);
        } else {
            log.debug("No active polling found for event: {}", eventId);
        }
    }

    /** Returns an unmodifiable view of currently live event IDs (useful for monitoring/testing). */
    public Set<String> getLiveEvents() {
        return Collections.unmodifiableSet(liveEvents);
    }

    /** Returns the number of actively scheduled tasks (useful for testing). */
    public int getActiveTaskCount() {
        return scheduledTasks.size();
    }
}
