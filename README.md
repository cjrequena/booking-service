# Booking Service - Event Sourcing & CQRS Microservices

## Abstract

The Booking Service is a microservices-based system implementing Event Sourcing and CQRS (Command Query Responsibility Segregation) for managing booking operations. It provides strong consistency on the write side, a complete audit trail, and an independently scalable read side.

The project demonstrates Domain-Driven Design (DDD), Event Sourcing, CQRS, live read-model updates over Server-Sent Events, and event-driven integration through Apache Kafka using CloudEvents.

---

## Overview

The repository is a Maven multi-module build:

| Module | Role |
|--------|------|
| **es-core** | Shared event-sourcing library: event store, snapshots, subscriptions, `AggregateLoader`, Flyway migrations for the event store schema |
| **booking-command-handler** | Write side: validates commands, applies them to the `Booking` aggregate, appends events to the event store, publishes events to Kafka |
| **booking-query-handler** | Read side: projects events into MongoDB, serves queries (RSQL filtering, sorting, paging), streams live updates via SSE, manages projection dead letters |

### Key Features

- **Event Sourcing**: All booking state changes stored as immutable events in PostgreSQL
- **CQRS**: Separate write and read models, each scaled independently
- **Domain-Driven Design**: `Booking` aggregate with value objects for paxes and products (Transfer, Hotel)
- **Optimistic Concurrency Control**: Aggregate version check on append prevents lost updates
- **Snapshots**: Configurable per aggregate type (default: every 5 events)
- **Shared Aggregate Loading**: One snapshot-or-replay path (`AggregateLoader`) used by command and event handlers
- **Kafka Integration Events**: CloudEvents 1.0 (binary mode) published through a decoupled emitter, at-least-once
- **Projection Dead Letter**: Failing aggregates are recorded and can be replayed or discarded without stalling the subscription
- **Live Updates**: `GET /bookings/stream` (SSE), fanned out across instances over Redis pub/sub
- **RSQL Filtering**: Allowlisted filter and sort fields, optional offset/limit paging
- **Security**: Optional Cognito JWT resource server (disabled by default)
- **OpenAPI**: Swagger UI per service, plus static specs in `*/openapi/openapi.yaml`
- **Observability**: Actuator, Micrometer tracing, health checks
- **Code Quality**: JaCoCo coverage gate and SonarQube integration

### Technology Stack

**Core Framework:**
- Java 25
- Spring Boot 4.1.2
- Spring Cloud 2025.1.4
- Spring Web MVC (blocking)
- Jackson 3 (`tools.jackson`)
- Spring Data JPA & Spring Data MongoDB

**Event Store & Messaging:**
- PostgreSQL (Event Store, Flyway-managed schema)
- Apache Kafka via Spring Cloud Stream (integration events)
- MongoDB (Read Model Projections)

**Caching & Fan-out:**
- Redis (projection cache, SSE pub/sub)

**Security:**
- Spring Security OAuth2 Resource Server (AWS Cognito JWT)

**Observability:**
- Spring Boot Actuator
- Micrometer Tracing

**Development & Quality:**
- Maven 3.9+
- Lombok, MapStruct
- JUnit 5, Mockito, Testcontainers
- JaCoCo, SonarQube
- Docker & Docker Compose

---

## Architecture

### High-Level Design

```mermaid
graph TB
    Client["Client Applications"]

    Client -->|"Commands (POST)"| RestCmd
    Client -->|"Queries (GET) / SSE"| RestQuery

    subgraph CMD["booking-command-handler (Port 8080)"]
        direction TB
        RestCmd["REST Controllers"] --> CommandBus["Command Bus"]
        CommandBus --> Aggregates["Booking Aggregate"]
        Aggregates --> EventStoreService["Event Store Service (es-core)"]
        CmdSub["Event Subscription (polling)"] --> Emitter["KafkaEventEmitter"]
    end

    subgraph QRY["booking-query-handler (Port 7080)"]
        direction TB
        RestQuery["REST / SSE Controllers"] --> QueryService["BookingProjectionService"]
        QuerySub["Event Subscription (polling)"] --> Projection["Projection Handler"]
        Projection --> MongoDB
        QueryService --> MongoDB[("MongoDB\nbooking, dead_letter_projection")]
        Projection -->|"publish update"| Redis[("Redis\ncache + pub/sub")]
        Redis -->|"fan-out"| RestQuery
    end

    EventStoreService --> PostgreSQL[("PostgreSQL\nEvent Store")]
    PostgreSQL -.->|"poll"| CmdSub
    PostgreSQL -.->|"poll"| QuerySub
    Emitter -->|"CloudEvents"| Kafka["Apache Kafka\nevent-booking-01"]
```

### Projection Synchronization

Both services can run an event-store subscription (`eventstore.subscription.enabled`). Which service writes projections is controlled by `projections.handlers.enabled`:

| Property | command-handler | query-handler |
|----------|-----------------|---------------|
| `projections.handlers.enabled` (default, recommended) | `false` | `true` |

**Default flow:**
1. Command handler validates the command and appends events to the event store
2. Query handler's `ScheduledEventHandlerService` polls the event store (`PT6S`)
3. `BookingEventHandler` rebuilds the aggregate (snapshot + replay) and upserts the MongoDB projection
4. The update is published on Redis and pushed to SSE subscribers
5. Failures are classified per aggregate: transient infrastructure errors are rethrown so the batch retries; anything else is written to `dead_letter_projection` and the subscription moves on

The alternative (command handler writes projections, query handler only reads) is still supported by flipping both flags. See [Projection Synchronization Guide](.docs/PROJECTION_SYNCHRONIZATION_GUIDE.md).

### Kafka Integration Events

Kafka is used for **external boundaries**, not for internal projection synchronization.

- The command handler's subscription hands each event to `ApplicationEventPublisher`; `KafkaEventEmitter` listens and sends it
- `EventTypeBindings` routes each event type to binding `event-booking-out-0` → topic `event-booking-01`, and fails loudly on unmapped types
- CloudEvents 1.0 binary mode: `ce_specversion`, `ce_id`, `ce_type`, `ce_source`, `ce_time` headers, `content-type` as data content type, message key = aggregate id
- The producer is synchronous and runs inside the subscription poll transaction, so a failed send rolls back the offset and the batch is retried (**at-least-once**; consumers must be idempotent)

### Domain Model

```
Booking Aggregate Root
├── Aggregate ID / Version
├── Booking ID, Booking Reference
├── Status: CREATED | CONFIRMED | CANCELLED | COMPLETED | EXPIRED
├── Paxes (List<PaxVO>)
├── Lead Pax ID
├── Products (List<ProductVO>)
│   ├── TransferVO (origin, destination, trips, vehicle, price)
│   └── HotelVO (hotel, location, check-in/out, rooms, price)
└── Metadata

Commands: Create, Confirm, Cancel, Complete, Expire
Events:   BookingCreated, BookingConfirmed, BookingCancelled, BookingCompleted, BookingExpired
```

See [Booking and Product Status Lifecycle](.docs/BOOKING_AND_PRODUCT_STATUS_LIFECYCLE_SPECIFICATION.md).

---

## How to Compile

### Prerequisites

- **Java 25** (`.sdkmanrc`: `java=25-tem`)
- **Maven 3.9+**
- **Docker** (infrastructure and Testcontainers integration tests)

### Build

```bash
# Full build (unit + integration tests, JaCoCo gate)
mvn clean verify

# Install all modules
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Single module (with its dependencies)
mvn -pl booking-command-handler -am verify

# Skip integration tests only
mvn clean verify -DskipIntegrationTests=true
```

Integration tests (`*IT.java`) start PostgreSQL, MongoDB and Kafka with Testcontainers, so Docker must be running. The JaCoCo gate requires 10% minimum coverage per module.

---

## How to Run

### Option 1: Maven (Development)

```bash
# 1. Infrastructure
cd .docker
docker compose up -d postgres mongo kafka redis

# 2. Command handler (port 8080)
mvn -pl booking-command-handler spring-boot:run

# 3. Query handler (port 7080), in another terminal
mvn -pl booking-query-handler spring-boot:run
```

The `local` profile is active by default. Flyway creates the `eventstore` schema on first start.

### Option 2: Executable JARs

```bash
mvn clean package -DskipTests
java -jar booking-command-handler/target/booking-command-handler-exec.jar
java -jar booking-query-handler/target/booking-query-handler-exec.jar
```

### Option 3: Docker Compose (Full Stack)

The service images copy the pre-built `*-exec.jar`, so build first:

```bash
mvn clean package -DskipTests
cd .docker
docker compose up -d --build
```

| Component | Port |
|-----------|------|
| booking-command-handler | 8080 |
| booking-query-handler | 7080 |
| PostgreSQL (Event Store) | 5432 |
| MongoDB | 27017 |
| Mongo Express | 8081 |
| Kafka | 9092 |
| Kafbat UI | 18080 |
| Redis | 6379 |
| SonarQube | 9000 |

### Main Environment Variables

| Variable | Default | Used by |
|----------|---------|---------|
| `SERVER_PORT` | `8080` / `7080` | both |
| `SPRING_PROFILES_ACTIVE` | `local` | both |
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/postgres` | both |
| `MONGODB_URI` | `mongodb://root:root@127.0.0.1:27017/booking?authSource=admin` | both |
| `KAFKA_BROKERS` | `localhost:9092` | command |
| `REDIS_HOST` | `localhost` | query |
| `COGNITO_ENABLED` | `false` | both |
| `COGNITO_ISSUER_URI` | _(empty)_ | both |

### Security

Security is disabled by default (all requests permitted). Set `COGNITO_ENABLED=true` and `COGNITO_ISSUER_URI` to validate bearer JWTs. `/management/**`, Swagger UI and `/v3/api-docs/**` stay public.

### Verify

```bash
curl http://localhost:8080/management/healthcheck
curl http://localhost:7080/management/healthcheck
```

**Create a booking (Transfer + Hotel):**
```bash
curl -X POST http://localhost:8080/command-handler/api/bookings/create \
  -H "Accept-Version: application/vnd.booking-command-handler.v1" \
  -H "Content-Type: application/json" \
  -d '{
    "paxes": [{
      "pax_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john@example.com",
      "phone": "+1234567890",
      "age": 30,
      "document_type": "PASSPORT",
      "document_number": "AB123456",
      "pax_type": "ADULT"
    }],
    "lead_pax_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "products": [
      {
        "product_type": "Transfer",
        "product_id": "11111111-1111-1111-1111-111111111111",
        "search_id": "aaaa1111-bbbb-cccc-dddd-eeee11111111",
        "search_created_at": "2026-06-01T08:00:00.000+00:00",
        "paxes_ids": ["a1b2c3d4-e5f6-7890-abcd-ef1234567890"],
        "origin": { "latitude": 39.55, "longitude": 2.73, "iata_code": "PMI", "full_address": "Palma Airport" },
        "destination": { "latitude": 39.69, "longitude": 3.01, "full_address": "Hotel Grand Mallorca" },
        "outbound_trip": {
          "trip_id": "aa000001-0001-0001-0001-000000000001",
          "pickup_datetime": "2026-06-15T10:30:00.000+00:00",
          "transfer_type": "ONE_WAY",
          "vehicle": {
            "vehicle_id": "bb000001-0001-0001-0001-000000000001",
            "type": "SEDAN", "description": "Mercedes E-Class", "model": "Mercedes E-Class",
            "capacity": 4, "max_bags": 3, "max_paxes": 3
          }
        },
        "price": { "service_type": "PRIVATE", "currency": "EUR", "total_amount": 65.00, "subtotal_amount": 55.00, "fees_and_taxes": 10.00 }
      },
      {
        "product_type": "Hotel",
        "product_id": "22222222-2222-2222-2222-222222222222",
        "search_id": "aaaa1111-bbbb-cccc-dddd-eeee11111111",
        "search_created_at": "2026-06-01T08:00:00.000+00:00",
        "paxes_ids": ["a1b2c3d4-e5f6-7890-abcd-ef1234567890"],
        "hotel_name": "Hotel Grand Mallorca",
        "hotel_code": "HGM-001",
        "location": { "latitude": 39.69, "longitude": 3.01, "full_address": "Av. de Sa Coma 12, Cala Millor" },
        "check_in": "2026-06-15T14:00:00.000+00:00",
        "check_out": "2026-06-20T11:00:00.000+00:00",
        "rooms": [{ "room_id": "cc000001-0001-0001-0001-000000000001", "room_type": "DOUBLE_SEA_VIEW", "room_description": "Double room with sea view", "quantity": 1, "max_occupancy": 2 }],
        "price": { "currency": "EUR", "total_amount": 875.00, "subtotal_amount": 750.00, "fees_and_taxes": 125.00, "nightly_rate": 150.00, "nights": 5 }
      }
    ],
    "metadata": { "source": "web", "channel": "direct" }
  }'
```

**Query it:**
```bash
curl http://localhost:7080/query-handler/api/bookings/{bookingId} \
  -H "Accept-Version: application/vnd.booking-query-handler.v1"
```

**Follow live updates:**
```bash
curl -N http://localhost:7080/query-handler/api/bookings/stream \
  -H "Accept-Version: application/vnd.booking-query-handler.v1"
```

A Postman collection is available at [booking-service.postman_collection.json](booking-service.postman_collection.json).

---

## API Endpoints

### Command Handler (Port 8080)

**Base URL:** `http://localhost:8080/command-handler/api/bookings`
**Header:** `Accept-Version: application/vnd.booking-command-handler.v1`

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| POST | `/create` | Create a booking | 201 |
| POST | `/{bookingId}/confirm` | Confirm a booking | 200 |
| POST | `/{bookingId}/cancel` | Cancel a booking | 200 |
| POST | `/{bookingId}/complete` | Complete a booking | 200 |
| POST | `/{bookingId}/expire` | Expire a booking | 200 |

### Query Handler (Port 7080)

**Base URL:** `http://localhost:7080/query-handler/api`
**Header:** `Accept-Version: application/vnd.booking-query-handler.v1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/bookings/{bookingId}` | Get a booking projection by ID |
| GET | `/bookings` | List bookings (`filter`, `sort`, `offset`, `limit`) |
| GET | `/bookings/stream` | SSE stream of booking updates (optional `booking_id`) |
| GET | `/dead-letter-projections` | List dead-lettered projections (`status`, `aggregate_type`, paging) |
| GET | `/dead-letter-projections/{id}` | Get a dead letter |
| POST | `/dead-letter-projections/{id}/replay` | Re-project the aggregate from the event store |
| POST | `/dead-letter-projections/{id}/discard` | Close without replaying |

Details: [Command Handler README](booking-command-handler/README.md) · [Query Handler README](booking-query-handler/README.md)

---

## How to Check SonarQube

```bash
cd .docker
docker compose up -d sonarqube sonarqube-db
```

1. Open http://localhost:9000 (default `admin` / `admin`, change on first login)
2. **My Account → Security → Generate Tokens** (type: Project Analysis Token)
3. Run the analysis:

```bash
mvn clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<YOUR_TOKEN_HERE>
```

The Sonar plugin, project key (`${project.artifactId}`), exclusions and JaCoCo report paths (unit and IT) are configured in the root `pom.xml`.

---

## Additional Resources

### Documentation

- [Command Handler README](booking-command-handler/README.md)
- [Query Handler README](booking-query-handler/README.md)
- [Contributing Guide](CONTRIBUTING.md)
- [Projection Synchronization Guide](.docs/PROJECTION_SYNCHRONIZATION_GUIDE.md)
- [Agentic AI Integration Architecture](.docs/AGENTIC_AI_BOOKING_SERVICE.md)
- [Booking and Product Status Lifecycle](.docs/BOOKING_AND_PRODUCT_STATUS_LIFECYCLE_SPECIFICATION.md)

### API Documentation

| | Command Handler | Query Handler |
|--|-----------------|---------------|
| Swagger UI | http://localhost:8080/swagger-ui.html | http://localhost:7080/swagger-ui.html |
| OpenAPI (live) | http://localhost:8080/v3/api-docs | http://localhost:7080/v3/api-docs |
| OpenAPI (static) | [openapi.yaml](booking-command-handler/openapi/openapi.yaml) | [openapi.yaml](booking-query-handler/openapi/openapi.yaml) |

### Monitoring & Management

- **Command Handler Actuator**: http://localhost:8080/management
- **Query Handler Actuator**: http://localhost:7080/management
- **Mongo Express**: http://localhost:8081
- **Kafbat UI**: http://localhost:18080
- **SonarQube**: http://localhost:9000

### External Links

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/)
- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [CloudEvents Kafka Binding](https://github.com/cloudevents/spec/blob/main/cloudevents/bindings/kafka-protocol-binding.md)
- [RSQL Parser](https://github.com/jirutka/rsql-parser)

---

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
