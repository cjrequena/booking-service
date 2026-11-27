# Booking Command Handler - Write Side Microservice

## Abstract

The Booking Command Handler is the write side of the CQRS architecture. It validates booking commands, applies them to the `Booking` aggregate, appends the resulting events to the PostgreSQL event store, and publishes them to Apache Kafka as CloudEvents for external consumers.

---

## Overview

The event store is the single source of truth for booking state. The read model is maintained by `booking-query-handler`, which polls the same event store.

### Key Features

- **Command Processing**: Create, Confirm, Cancel, Complete, Expire
- **Event Sourcing**: Append-only event store from `es-core`
- **Optimistic Concurrency Control**: Aggregate version check on append; conflicts return `409`
- **Snapshots**: `Booking` snapshot every 5 events (configurable)
- **Kafka Integration Events**: CloudEvents 1.0 binary mode, at-least-once, via a decoupled `KafkaEventEmitter`
- **Optional Projections**: Can write MongoDB projections when `projections.handlers.enabled=true` (disabled by default)
- **Validation**: Jakarta Bean Validation on all request DTOs
- **Security**: Optional Cognito JWT resource server
- **OpenAPI**: Swagger UI and a static spec in [`openapi/openapi.yaml`](openapi/openapi.yaml)

### Technology Stack

- Java 25, Spring Boot 4.1.2, Spring Web MVC
- Jackson 3
- Spring Data JPA, HikariCP, Flyway (event store, via `es-core`)
- PostgreSQL (event store)
- Spring Cloud Stream Kafka binder
- Spring Data MongoDB (optional projections)
- Spring Security OAuth2 Resource Server
- Lombok, MapStruct
- JUnit 5, Mockito, Testcontainers (PostgreSQL, MongoDB, Kafka)

---

## Architecture

### Component Structure

```
booking-command-handler/
├── controller/          # BookingCommandController, DTOs, GlobalExceptionHandler
├── service/
│   ├── command/         # CommandBusService and one handler per command
│   ├── event/           # ScheduledEventHandlerService, BookingEventHandler
│   └── projection/      # Optional MongoDB projection handler
├── messaging/kafka/     # KafkaEventEmitter, EventTypeBindings, header mapper, serializer
├── domain/
│   ├── model/           # Booking aggregate, commands, events, value objects (Transfer, Hotel)
│   ├── mapper/
│   └── exception/
├── persistence/mongodb/ # Projection entities and repository
├── configuration/       # Jackson, Web MVC, OpenAPI, Mongo, security, REST client
└── shared/              # Constants, serializers, utilities
```

### Command Flow

```
Client → BookingCommandController (validation)
       → CommandBusService → <Command>Handler
       → AggregateLoader (snapshot + replay, es-core)
       → Booking.applyCommand(...) → new events
       → EventStoreService.saveAggregate(...)  # optimistic version check, own transaction
       → 201/200 { booking_id, status } + Booking-Id header
```

`CommandBusService` and command handlers are not `@Transactional`; `EventStoreService` owns its transactions and the version check guards against stale reads.

### Event Publication Flow

```
ScheduledEventHandlerService (polls event store, PT6S)
  → BookingEventHandler
  → ApplicationEventPublisher
  → KafkaEventEmitter (@EventListener)
  → binding event-booking-out-0 → topic event-booking-01
```

- `EventTypeBindings` maps each event type to its binding and throws on unmapped types
- Wire format: CloudEvents 1.0 binary mode — `ce_specversion`, `ce_id`, `ce_type`, `ce_source`, `ce_time`, `content-type`; key = aggregate id
- `KafkaHeaderMapperConfiguration` keeps Spring framework headers off the wire; tracing headers (`b3`, `traceparent`, `tracestate`, `correlationid`) are allowed
- `KafkaJsonSerializer` uses Jackson 3 with Jackson 2-compatible defaults, so the payload shape is unchanged
- The producer is synchronous (`sync: true`) and runs inside the poll transaction: a failed send rolls back the subscription offset and the batch is retried (**at-least-once**)
- Producer timeouts are bounded (`max.block.ms` 5s, `delivery.timeout.ms` 10s) so an unreachable broker fails fast

### Domain Model

```java
public class Booking extends Aggregate {
    private UUID bookingId;
    private String bookingReference;
    private BookingStatus status;
    private List<PaxVO> paxes;
    private UUID leadPaxId;
    private List<ProductVO> products;   // TransferVO, HotelVO
    private MetadataVO metadata;
}
```

**Status lifecycle:** `CREATED → CONFIRMED → COMPLETED`, with `CANCELLED` and `EXPIRED` as terminal alternatives. See [Booking and Product Status Lifecycle](../.docs/BOOKING_AND_PRODUCT_STATUS_LIFECYCLE_SPECIFICATION.md).

---

## How to Compile

### Prerequisites

- Java 25
- Maven 3.9+
- Docker (for integration tests and local infrastructure)

### Build

```bash
# From the repository root
mvn -pl booking-command-handler -am clean verify     # unit + integration tests
mvn -pl booking-command-handler -am clean package -DskipTests
```

Artifact: `target/booking-command-handler-exec.jar`

---

## How to Run

### Maven

```bash
cd .docker && docker compose up -d postgres mongo kafka
cd .. && mvn -pl booking-command-handler spring-boot:run
```

### JAR

```bash
java -jar booking-command-handler/target/booking-command-handler-exec.jar
```

### Docker

```bash
mvn -pl booking-command-handler -am clean package -DskipTests
cd .docker && docker compose up -d --build booking-command-handler
```

### Configuration

Base settings are in `application.yml`; the `local` profile (`application-local.yml`) holds infrastructure settings.

| Property / Variable | Default | Description |
|---------------------|---------|-------------|
| `SERVER_PORT` | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | `local` | Active profile |
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/postgres` | Event store (schema `eventstore`) |
| `MONGODB_URI` | `mongodb://root:root@127.0.0.1:27017/booking?authSource=admin` | Projection DB (`spring.mongodb.*`) |
| `KAFKA_BROKERS` | `localhost:9092` | Kafka brokers |
| `COGNITO_ENABLED` / `COGNITO_ISSUER_URI` | `false` / empty | JWT authentication |
| `projections.handlers.enabled` | `false` | Write MongoDB projections from this service |
| `eventstore.subscription.enabled` | `true` | Run the polling subscription (needed for Kafka publishing) |
| `eventstore.subscription.polling-interval` | `PT6S` | Poll interval |
| `eventstore.snapshot.Booking.interval` | `5` | Events between snapshots |
| `spring.flyway.locations` | `classpath:/db/eventstore-migration` | Event store migrations (in `es-core`) |

### Verify

```bash
curl http://localhost:8080/management/healthcheck
open http://localhost:8080/swagger-ui.html
```

---

## API Endpoints

**Base URL:** `http://localhost:8080/command-handler/api/bookings`
**Required header:** `Accept-Version: application/vnd.booking-command-handler.v1`

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| POST | `/create` | Create a booking (body: paxes, lead_pax_id, products, metadata) | 201 |
| POST | `/{bookingId}/confirm` | Confirm | 200 |
| POST | `/{bookingId}/cancel` | Cancel | 200 |
| POST | `/{bookingId}/complete` | Complete | 200 |
| POST | `/{bookingId}/expire` | Expire | 200 |

**Response body:**
```json
{ "booking_id": "uuid", "status": "CREATED" }
```
The `Booking-Id` response header carries the aggregate id. A full create example (Transfer + Hotel) is in the [root README](../README.md#verify).

### Error Responses

| Status | Cause |
|--------|-------|
| 400 | Validation error or business rule violation |
| 401 | Missing/invalid JWT (only when Cognito is enabled) |
| 404 | Booking not found, or unknown path |
| 409 | Optimistic concurrency conflict |
| 500 | Unexpected error |
| 501 | No handler registered for the command |
| 503 | Downstream dependency unavailable |

---

## Testing

| Type | Location | Notes |
|------|----------|-------|
| Unit | `domain/`, `service/`, `controller/exception/` | Aggregate, commands, value objects, command handlers, projection, exception handling |
| Integration | `integration/BookingCommandHandlerIT` | End-to-end command flow against Testcontainers PostgreSQL + MongoDB |
| Integration | `integration/KafkaEventPublicationIT` | Event → Kafka CloudEvents publication against Testcontainers Kafka |

```bash
mvn -pl booking-command-handler -am test                         # unit tests
mvn -pl booking-command-handler -am verify                       # + integration tests, JaCoCo gate
mvn -pl booking-command-handler test -Dtest=BookingTest          # single class
open booking-command-handler/target/site/jacoco/index.html
```

---

## Additional Resources

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI**: http://localhost:8080/v3/api-docs · [static spec](openapi/openapi.yaml)
- **Actuator**: http://localhost:8080/management (health: `/management/healthcheck`)
- **Kafbat UI**: http://localhost:18080 (inspect topic `event-booking-01`)

### Event Store Schema

Managed by Flyway from `es-core` (`db/eventstore-migration`), schema `eventstore`:

| Table | Purpose |
|-------|---------|
| `ES_AGGREGATE` | Aggregate id, type and current version |
| `ES_EVENT` | Appended events (with transaction-id offset for subscriptions) |
| `ES_AGGREGATE_SNAPSHOT` | Aggregate snapshots |
| `ES_EVENT_SUBSCRIPTION` | Subscription offsets |

---

## License

This project is licensed under the terms specified in the [LICENSE](../LICENSE) file.
