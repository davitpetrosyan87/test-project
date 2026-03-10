package com.example.eventscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventScoreTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventScoreTrackerApplication.class, args);
    }
}
