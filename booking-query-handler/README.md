# Booking Query Handler - Read Side Microservice

## Abstract

The Booking Query Handler is the read side of the CQRS architecture. It polls the PostgreSQL event store, projects booking events into MongoDB, and serves queries with RSQL filtering, sorting and paging. It pushes live updates to clients over Server-Sent Events and isolates failing projections in a dead-letter collection that operators can replay or discard.

---

## Overview

### Key Features

- **Event Store Polling**: `ScheduledEventHandlerService` reads new events every `PT6S`
- **MongoDB Projections**: Denormalized `booking` documents (Transfer and Hotel products)
- **RSQL Filtering**: Allowlisted filter and sort fields, optional offset/limit paging
- **Live Updates (SSE)**: `GET /bookings/stream`, fanned out across instances over Redis pub/sub, with local-broadcast fallback when Redis is down
- **Projection Dead Letter**: Per-aggregate failure classification; non-transient failures are recorded and the subscription keeps going
- **Redis Cache**: Booking-by-id lookups cached and refreshed on projection writes
- **Security**: Optional Cognito JWT resource server
- **OpenAPI**: Swagger UI and a static spec in [`openapi/openapi.yaml`](openapi/openapi.yaml)

### Technology Stack

- Java 25, Spring Boot 4.1.2, Spring Web MVC (blocking)
- Jackson 3
- Spring Data MongoDB
- Spring Data JPA (event store read access via `es-core`)
- Spring Data Redis (cache + pub/sub)
- RSQL Parser 2.1.0
- Spring Security OAuth2 Resource Server
- Lombok, MapStruct, JUnit 5, Mockito

---

## Architecture

### Component Structure

```
booking-query-handler/
├── controller/          # BookingQueryController, BookingSSEController, DeadLetterProjectionController
├── service/
│   ├── base/            # BaseService: RSQL filter, sort, paging
│   ├── event/           # ScheduledEventHandlerService, BookingEventHandler, DeadLetterProjectionService
│   ├── projection/      # BookingProjectionService, BookingProjectionHandler
│   └── sse/             # BookingSSEService, BookingUpdatePublisher/Subscriber
├── persistence/mongodb/ # BookingEntity, DeadLetterProjectionEntity, repositories
├── domain/              # Booking aggregate, events, value objects, PageResult
├── configuration/       # Jackson, Web MVC, Mongo, Redis, security
└── shared/common/       # RSQL criteria builder, sort utils, serializers
```

### Projection Flow

```
ScheduledEventHandlerService (poll, PT6S)
  → BookingEventHandler
      → AggregateLoader (snapshot + replay, es-core)
      → BookingProjectionService.save(...)        # upsert MongoDB, refresh Redis cache
      → BookingUpdatePublisher                    # Redis channel booking-query-handler:sse:booking-updates
  ↳ on failure, per aggregate:
      transient (DB/broker unreachable, timeouts) → rethrow, batch retried
      anything else                               → dead_letter_projection (PENDING), continue
```

### SSE Fan-out

```
Projection write → Redis pub/sub → BookingUpdateSubscriber (every instance) → BookingSSEService → clients
```

- Event name: `booking-update`, data: `{ "booking_id": "...", "status": "..." }` — a hint to refetch the booking
- A `keepalive` comment is sent every 15s so proxies don't drop idle connections
- If Redis is unavailable, updates are broadcast to the local instance's clients only

---

## How to Compile

### Prerequisites

- Java 25
- Maven 3.9+

### Build

```bash
# From the repository root
mvn -pl booking-query-handler -am clean verify
mvn -pl booking-query-handler -am clean package -DskipTests
```

Artifact: `target/booking-query-handler-exec.jar`

---

## How to Run

### Maven

```bash
cd .docker && docker compose up -d postgres mongo redis
cd .. && mvn -pl booking-query-handler spring-boot:run
```

The command handler (or Flyway in it) must have created the `eventstore` schema first.

### JAR

```bash
java -jar booking-query-handler/target/booking-query-handler-exec.jar
```

### Docker

```bash
mvn -pl booking-query-handler -am clean package -DskipTests
cd .docker && docker compose up -d --build booking-query-handler
```

### Configuration

| Property / Variable | Default | Description |
|---------------------|---------|-------------|
| `SERVER_PORT` | `7080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | `local` | Active profile |
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/postgres` | Event store (schema `eventstore`) |
| `MONGODB_URI` | `mongodb://root:root@127.0.0.1:27017/booking?authSource=admin` | Projection DB (`spring.mongodb.*`) |
| `REDIS_HOST` | `localhost` | Redis (cache + SSE fan-out) |
| `COGNITO_ENABLED` / `COGNITO_ISSUER_URI` | `false` / empty | JWT authentication |
| `projections.handlers.enabled` | `true` | Write MongoDB projections from this service |
| `eventstore.subscription.enabled` | `true` | Run the polling subscription |
| `eventstore.subscription.name` | `bqh-mongo-projection-${random.int(100)}` | Subscription name (offset tracking) |
| `eventstore.subscription.polling-interval` | `PT6S` | Poll interval |

> The local subscription name is randomized, so each start re-projects from the beginning. Use a fixed name in deployed environments.

### Verify

```bash
curl http://localhost:7080/management/healthcheck
open http://localhost:7080/swagger-ui.html
```

---

## API Endpoints

**Base URL:** `http://localhost:7080/query-handler/api`
**Required header:** `Accept-Version: application/vnd.booking-query-handler.v1`

### Bookings

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/bookings/{bookingId}` | Booking projection by ID (`Cache-Control: private, max-age=600`) |
| GET | `/bookings` | List bookings |
| GET | `/bookings/stream` | SSE stream; optional `booking_id` to follow one booking |

**`GET /bookings` query parameters:**

| Parameter | Description | Example |
|-----------|-------------|---------|
| `filter` | RSQL over `booking_id`, `booking_reference`, `status`, `lead_pax_id` | `status==CONFIRMED;booking_reference==BK*` |
| `sort` | `booking_reference` and/or `status` | `status,asc;booking_reference,desc` |
| `offset` | Zero-based index of the first result (`page` = `offset / limit`) | `0` |
| `limit` | Page size, 1–200 | `10` |

- Operators: `==`, `!=`, `=in=`, `=out=`, `=gt=`, `=ge=`, `=lt=`, `=le=`, and `=like=` (case-insensitive substring, text fields only). `==` with `*` on a text field is a case-insensitive wildcard.
- Without `offset` and `limit` the response is a JSON array. With both, it is a page envelope:

```json
{ "content": [ ... ], "total_elements": 42, "total_pages": 5, "page": 0, "size": 10 }
```

```bash
curl -G http://localhost:7080/query-handler/api/bookings \
  -H "Accept-Version: application/vnd.booking-query-handler.v1" \
  --data-urlencode "filter=status==CONFIRMED" \
  --data-urlencode "sort=booking_reference,asc" \
  -d offset=0 -d limit=10

curl -N "http://localhost:7080/query-handler/api/bookings/stream?booking_id={bookingId}" \
  -H "Accept-Version: application/vnd.booking-query-handler.v1"
```

### Dead-Letter Projections

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dead-letter-projections` | List; filters `status`, `aggregate_type`; `offset`, `limit` (default 20, max 200), `sort` |
| GET | `/dead-letter-projections/{id}` | Get one |
| POST | `/dead-letter-projections/{id}/replay` | Re-project the aggregate from the event store |
| POST | `/dead-letter-projections/{id}/discard` | Close without replaying |

**Statuses:** `PENDING` (recorded) → `REPLAYED` (success) or `FAILED` (replay failed again, can be retried) · `DISCARDED` (closed by operator). Replaying or discarding a closed entry returns `409`.

### Error Responses

| Status | Cause |
|--------|-------|
| 400 | Invalid filter, sort, offset or limit |
| 401 | Missing/invalid JWT (only when Cognito is enabled) |
| 404 | Booking or dead letter not found, or unknown path |
| 409 | Invalid dead-letter status transition |
| 500 | Unexpected error |
| 503 | Downstream dependency unavailable |

---

## Testing

| Test | Covers |
|------|--------|
| `RsqlMongoCriteriaBuilderTest` | RSQL → Mongo `Criteria` translation |
| `SortUtilsTest` | Sort parsing and allowlisting |
| `BookingEventHandlerTest` | Transient vs. dead-letter failure classification |
| `DeadLetterProjectionServiceTest` | Replay and discard |
| `BookingProjectionServiceTest` | Save + publish, filtering and paging |
| `BookingUpdateFanOutTest` | Redis SSE fan-out and local fallback |
| `GlobalExceptionHandlerTest` | Error mapping |

```bash
mvn -pl booking-query-handler -am verify
open booking-query-handler/target/site/jacoco/index.html
```

---

## Additional Resources

- **Swagger UI**: http://localhost:7080/swagger-ui.html
- **OpenAPI**: http://localhost:7080/v3/api-docs · [static spec](openapi/openapi.yaml)
- **Actuator**: http://localhost:7080/management (health: `/management/healthcheck`)
- **Mongo Express**: http://localhost:8081

### MongoDB Collections (database `booking`)

| Collection | Content |
|------------|---------|
| `booking` | Booking projections with embedded paxes and products |
| `dead_letter_projection` | `aggregate_id`, `aggregate_type`, `status`, `attempts`, `error_message`, `last_error`, timestamps |

---

## License

This project is licensed under the terms specified in the [LICENSE](../LICENSE) file.
