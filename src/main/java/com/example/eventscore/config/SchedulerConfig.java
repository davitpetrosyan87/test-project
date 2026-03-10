package com.example.eventscore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("score-poller-");
        scheduler.setErrorHandler(t ->
                org.slf4j.LoggerFactory.getLogger("SchedulerErrorHandler")
                        .error("Unexpected error in scheduled task", t));
        return scheduler;
    }
}
