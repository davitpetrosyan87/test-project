# Event Score Tracker

A Java Spring Boot microservice that tracks live sports events and periodically polls an external API for score updates, publishing them to Kafka.

## Prerequisites

- Java 17+ (tested with Java 21)
- Maven 3.9+ (or use the included Maven Wrapper `./mvnw`)
- Docker & Docker Compose (for Kafka)

## Setup & Run

### 1. Start Kafka

```bash
docker-compose up -d
```

This starts a single-node Kafka broker (KRaft mode, no Zookeeper) on `localhost:9092`.

### 2. Run the Application

With the mock external API (recommended for demo):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock-api
```

Or with Maven directly:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mock-api
```

The `mock-api` profile enables a built-in mock endpoint at `/events/{eventId}/score` that simulates score changes. Without this profile, configure `app.external-api.base-url` to point to a real external API.

### 3. Use the API

Set an event as live (starts polling every 10 seconds):

```bash
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "match-1", "status": "live"}'
```

Stop polling for an event:

```bash
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "match-1", "status": "not live"}'
```

### 4. Verify Kafka Messages

```bash
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic event-scores \
  --from-beginning
```

## Running Tests

```bash
./mvnw test
```

or

```bash
mvn test
```

Tests include:
- **Unit tests**: Controller validation, scheduler service logic, Kafka publisher, score polling service
- **Integration test**: Full end-to-end flow with embedded Kafka — sets event live, verifies Kafka message, then stops polling

All tests use an embedded Kafka broker and require no external infrastructure.

## Configuration

Key properties in `application.yml`:

| Property | Default | Description |
|---|---|---|
| `app.external-api.base-url` | `http://localhost:8081` | External score API base URL |
| `app.kafka.topic` | `event-scores` | Kafka topic for score events |
| `app.polling.interval-seconds` | `10` | Polling interval in seconds |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |

## Design Decisions

### Architecture

The service has three layers:
1. **REST Controller** (`EventController`) — Accepts status updates with Jakarta Bean Validation
2. **Scheduler Service** (`EventSchedulerService`) — Dynamically starts/stops per-event polling tasks
3. **Score Polling + Kafka Publishing** (`ScorePollingService`, `KafkaPublisherService`) — Fetches scores and publishes to Kafka

### Dynamic Task Scheduling

Instead of `@Scheduled` (which is static/annotation-based), the service uses Spring's `TaskScheduler.scheduleAtFixedRate()` programmatically. Each live event gets its own `ScheduledFuture<?>` stored in a `ConcurrentHashMap`, enabling independent start/stop per event. `computeIfAbsent` ensures idempotent scheduling (setting "live" twice doesn't create duplicate tasks).

### Error Handling

- **External API errors**: Caught and logged in `ScorePollingService.pollAndPublish()` — exceptions are never rethrown to prevent cancellation of the scheduled task
- **Kafka failures**: Producer is configured with `retries=3` and `acks=all`. The `KafkaTemplate.send()` callback logs successes and failures
- **Input validation**: Jakarta Bean Validation with a `GlobalExceptionHandler` returning structured 400 responses
- **HTTP timeouts**: `RestClient` is configured with 5-second connect/read timeouts to avoid blocking scheduler threads

### Thread Safety

- `ConcurrentHashMap` for both event states and scheduled task references
- `computeIfAbsent` for atomic check-and-schedule operations
- Thread pool of 10 for the `TaskScheduler` (configurable for production)

### Mock External API

The `MockExternalApiController` (active only with `mock-api` profile) simulates an external score API. It randomly increments scores ~20% of the time to demonstrate changing data.

### Kafka Configuration

- **Serialization**: String keys (eventId), JSON values (ScoreEvent)
- **Retry**: 3 retries with `max.in.flight.requests.per.connection=1` for ordering guarantees
- **Message key**: Uses `eventId` as the Kafka message key, ensuring all updates for the same event go to the same partition

## AI Usage Documentation

This project was built with assistance from Claude (Anthropic's AI). Here is how AI was used:

### What was AI-generated
- Initial project structure and all source code files
- Test cases for controller, services, and integration testing
- Docker Compose configuration for Kafka (KRaft mode)
- This README

### How AI output was verified and improved
- **Compilation**: All code was compiled with `mvn compile` to catch syntax/type errors
- **Test execution**: All 17 tests were run and pass (`mvn test`)
- **Bug fixes during development**:
  - Fixed `@MockBean` import — Spring Boot 3.4 moved it to `@MockitoBean` under `org.springframework.test.context.bean.override.mockito`
  - Fixed `ScheduledFuture<?>` wildcard generic issue in Mockito stubs — switched from `when().thenReturn()` to `doReturn().when()` pattern
  - Fixed integration test server port resolution — `${local.server.port}` isn't available at bean creation time with `RANDOM_PORT`, so switched to `DEFINED_PORT` with explicit port configuration
- **Design review**: Verified that scheduled task error handling prevents task cancellation, confirmed thread-safety of `ConcurrentHashMap` operations, and validated Kafka producer retry configuration

## Project Structure

```
src/main/java/com/example/eventscore/
├── EventScoreTrackerApplication.java    # Main application
├── config/
│   ├── RestClientConfig.java            # External API RestClient bean
│   └── SchedulerConfig.java             # TaskScheduler thread pool
├── controller/
│   ├── EventController.java             # POST /events/status endpoint
│   └── MockExternalApiController.java   # Mock score API (mock-api profile)
├── dto/
│   ├── EventStatusRequest.java          # Input DTO with validation
│   ├── ExternalScoreResponse.java       # External API response DTO
│   └── ScoreEvent.java                  # Kafka message DTO
├── exception/
│   └── GlobalExceptionHandler.java      # Validation & error responses
└── service/
    ├── EventSchedulerService.java       # Dynamic task scheduling
    ├── KafkaPublisherService.java       # Kafka producer with logging
    └── ScorePollingService.java         # External API polling
```
