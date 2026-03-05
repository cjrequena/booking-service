# Booking Service - Event Sourcing & CQRS Microservices

## Abstract

The Booking Service is a comprehensive microservices-based system implementing Event Sourcing and CQRS (Command Query Responsibility Segregation) patterns for managing booking operations. The system is designed to handle high-volume booking transactions with strong consistency guarantees, complete audit trails, and optimized read/write performance through separated command and query responsibilities.

This project demonstrates enterprise-grade architecture patterns including Domain-Driven Design (DDD), Event Sourcing, CQRS, reactive programming, and event-driven communication using Apache Kafka.

---

## Overview

The Booking Service is split into two independent microservices:

1. **booking-command-handler**: Handles all write operations (commands) and maintains the event store
2. **booking-query-handler**: Handles all read operations (queries) with optimized projections

### Key Features

- **Event Sourcing**: Complete audit trail of all booking state changes stored as immutable events
- **CQRS Pattern**: Separated read and write models for optimal performance and scalability
- **Domain-Driven Design**: Rich domain model with aggregates, value objects, and domain events
- **Reactive Programming**: Non-blocking, asynchronous operations using Spring WebFlux
- **Event-Driven Architecture**: Asynchronous communication between services via Apache Kafka
- **Optimistic Concurrency Control**: Prevents lost updates in concurrent scenarios
- **Snapshot Support**: Configurable snapshot strategy for performance optimization
- **Projection Synchronization**: Automatic projection updates from event stream
- **RESTful APIs**: Well-documented REST endpoints with OpenAPI/Swagger
- **Comprehensive Testing**: 141 unit tests with 100% pass rate
- **Observability**: Distributed tracing, metrics, and health checks
- **Code Quality**: SonarQube integration for continuous code quality monitoring

### Technology Stack

**Core Framework:**
- Java 21
- Spring Boot 3.4.1
- Spring Cloud 2024.0.0
- Spring WebFlux (Reactive)
- Spring Data JPA & MongoDB Reactive

**Event Store & Messaging:**
- PostgreSQL 17 (Event Store)
- Apache Kafka (Event Streaming)
- MongoDB 8 (Read Model Projections)

**Caching & Performance:**
- Redis (Distributed Cache)
- Caffeine (Local Cache)

**Observability & Monitoring:**
- Micrometer (Metrics)
- Spring Boot Actuator
- Distributed Tracing (Brave)

**Development & Quality:**
- Maven 3.9+
- Lombok
- MapStruct
- JUnit 5
- Mockito
- SonarQube
- Docker & Docker Compose

---

## Architecture

### High-Level Design

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Applications                      │
└────────────┬────────────────────────────────────┬────────────────┘
             │                                    │
             │ Commands (POST/PUT/DELETE)         │ Queries (GET)
             │                                    │
             ▼                                    ▼
┌────────────────────────────┐      ┌────────────────────────────┐
│  booking-command-handler   │      │   booking-query-handler    │
│  (Write Side - Port 8080)  │      │  (Read Side - Port 8081)   │
│                            │      │                            │
│  ┌──────────────────────┐ │      │  ┌──────────────────────┐ │
│  │  REST Controllers    │ │      │  │  REST Controllers    │ │
│  └──────────┬───────────┘ │      │  └──────────┬───────────┘ │
│             │              │      │             │              │
│  ┌──────────▼───────────┐ │      │  ┌──────────▼───────────┐ │
│  │  Command Bus         │ │      │  │  Query Service       │ │
│  └──────────┬───────────┘ │      │  └──────────┬───────────┘ │
│             │              │      │             │              │
│  ┌──────────▼───────────┐ │      │  ┌──────────▼───────────┐ │
│  │  Domain Aggregates   │ │      │  │  MongoDB Reactive    │ │
│  │  (Booking)           │ │      │  │  (Projections)       │ │
│  └──────────┬───────────┘ │      │  └──────────▲───────────┘ │
│             │              │      │             │              │
│  ┌──────────▼───────────┐ │      │  ┌──────────┴───────────┐ │
│  │  Event Store Service │ │      │  │  Projection Handler  │ │
│  └──────────┬───────────┘ │      │  │  (Polls Events)      │ │
│             │              │      │  └──────────┬───────────┘ │
│  ┌──────────▼───────────┐ │      │             │              │
│  │  PostgreSQL          │◄├──────┼─────────────┘              │
│  │  (Event Store)       │ │      │  Scheduled Event Polling   │
│  └──────────────────────┘ │      │                            │
│                            │      └────────────────────────────┘
│  Optional: Kafka Publisher│
│  (External Boundaries)    │      ┌────────────────────────────┐
│             │              │      │   Apache Kafka (Optional)  │
│             └──────────────┼─────►│   External Event Stream    │
└────────────────────────────┘      │   - booking-events         │
                                    └────────────────────────────┘

```

### Projection Synchronization Strategies

The system supports two projection update strategies controlled by configuration:

#### Strategy 1: Query-Handler Pulls Events (Recommended)
```yaml
# booking-command-handler
projections.handlers.enabled: false

# booking-query-handler
projections.handlers.enabled: true
```

**Flow:**
1. Command-Handler receives commands
2. Updates aggregates and persists events to Event Store
3. Query-Handler's ScheduledEventHandlerService polls Event Store
4. BookingEventHandler processes new events
5. Rebuilds aggregates from events
6. Updates MongoDB and PostgreSQL projections
7. Exposes query endpoints

**Benefits:**
- Pure separation of concerns
- Query-Handler controls its own projection updates
- No direct coupling between services
- Easier to scale query handlers independently

**Layout:**
```
┌─────────────────────────────────────────────────────────────┐
│                   Command-Handler                           │
│                  (Pure Command Side)                        │
├─────────────────────────────────────────────────────────────┤
│  1. Receive command                                         │
│  2. Update aggregate                                        │
│  3. Persist events to Event Store ✅                        │
│  4. Done! (No projection logic)                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  Event Store  │
                    │  (PostgreSQL) │
                    └───────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Query-Handler                             │
│                  (Pure Query Side)                          │
├─────────────────────────────────────────────────────────────┤
│  1. ScheduledEventHandlerService polls Event Store          │
│  2. BookingEventHandler processes new events                │
│  3. Rebuilds aggregate from events                          │
│  4. Updates MongoDB projection                              │
│  5. Updates PostgreSQL projection                           │
│  6. Exposes query endpoints                                 │
└─────────────────────────────────────────────────────────────┘
```

#### Strategy 2: Command-Handler Pushes Projections
```yaml
# booking-command-handler
projections.handlers.enabled: true

# booking-query-handler
projections.handlers.enabled: false
```

**Flow:**
1. Command-Handler receives commands
2. Updates aggregates and persists events to Event Store
3. Command-Handler's ScheduledEventHandlerService polls Event Store
4. BookingEventHandler processes events and updates projections
5. Query-Handler reads from pre-built projections

**Benefits:**
- Immediate projection updates
- Simpler query-handler implementation
- Centralized projection logic

**Layout:**

**Top Section: Command Handler (Expanded)**
```
┌─────────────────────────────────────────────┐
│     Command-Handler                         │
│     (Command + Projection Updates)          │
├─────────────────────────────────────────────┤
│  1. Receive command                         │
│  2. Update aggregate                        │
│  3. Persist events to Event Store ✅        │
│  4. Return response                         │
│  ┌───────────────────────────────────┐     │
│  │ Background Process:               │     │
│  │ 5. ScheduledEventHandlerService   │     │
│  │ 6. BookingEventHandler            │     │
│  │ 7. Updates MongoDB projection     │     │
│  │ 8. Updates PostgreSQL projection  │     │
│  └───────────────────────────────────┘     │
└─────────────────────────────────────────────┘
```

**Middle Section: Projections**
```
         ┌───────────────────┐
         │   MongoDB         │
         │   (Projections)   │
         └───────────────────┘
```

**Bottom Section: Query Handler (Simplified)**
```
┌─────────────────────────────────────────────┐
│     Query-Handler                           │
│     (Pure Query Side)                       │
├─────────────────────────────────────────────┤
│  1. Receives query requests                 │
│  2. Reads from MongoDB projection           │
│  3. Returns results                         │
└─────────────────────────────────────────────┘
```

### Kafka Usage (Optional)

Kafka is used for publishing events to **external boundaries** (other microservices, external systems), not for internal projection synchronization:

```yaml
# Optional Kafka configuration for external event publishing
spring:
  cloud:
    stream:
      bindings:
        booking-events-out:
          destination: booking-events
          content-type: application/json
```

**Architecture Diagram**: [View on Excalidraw](https://excalidraw.com/)

### Domain Model Hierarchy

```
Booking Aggregate Root
├── Aggregate ID (UUID)
├── Aggregate Version (Long)
├── Booking ID (UUID)
├── Booking Reference (String)
├── Status (BookingStatus Enum)
│   ├── CREATED
│   ├── PLACED
│   ├── CONFIRMED
│   ├── COMPLETED
│   ├── CANCELLED
│   └── EXPIRED
├── Paxes (List<PaxVO>)
│   ├── Pax ID
│   ├── First Name
│   ├── Last Name
│   ├── Email
│   ├── Phone
│   ├── Age
│   ├── Document Type
│   ├── Document Number
│   └── Pax Type
├── Lead Pax ID (UUID)
├── Products (List<ProductVO>)
│   ├── Product ID
│   ├── Product Name
│   ├── Product Type
│   ├── Price
│   └── Currency
└── Metadata (Map<String, Object>)

Commands
├── CreateBookingCommand
├── PlaceBookingCommand
├── ConfirmBookingCommand
├── CancelBookingCommand
├── CompleteBookingCommand
└── ExpireBookingCommand

Events
├── BookingCreatedEvent
├── BookingPlacedEvent
├── BookingConfirmedEvent
├── BookingCancelledEvent
├── BookingCompletedEvent
└── BookingExpiredEvent
```

**Domain Model Diagram**: [View on Excalidraw](https://excalidraw.com/)

### Key Components

**Command Handler (Write Side):**
- **Controllers**: REST endpoints for command operations
- **Command Bus**: Routes commands to appropriate handlers
- **Command Handlers**: Execute business logic and generate events
- **Domain Aggregates**: Encapsulate business rules and state
- **Event Store Service**: Persists events to PostgreSQL
- **Event Publisher**: Publishes events to Kafka
- **Projection Handler**: Updates MongoDB projections

**Query Handler (Read Side):**
- **Controllers**: REST endpoints for query operations
- **Query Service**: Retrieves data from MongoDB projections
- **MongoDB Repository**: Reactive data access layer
- **Event Consumer**: Listens to Kafka events and updates projections

---

## How to Compile

### Prerequisites

- **Java 21** or higher
- **Maven 3.9+**
- **Docker & Docker Compose** (for infrastructure)
- **Git**

### Compilation Steps

1. **Clone the repository:**
```bash
git clone <repository-url>
cd booking-service
```

2. **Compile the entire project:**
```bash
mvn clean install
```

3. **Compile individual modules:**
```bash
# Command Handler
cd booking-command-handler
mvn clean package

# Query Handler
cd booking-query-handler
mvn clean package
```

4. **Run tests:**
```bash
# All tests
mvn test

# Specific module
mvn test -pl booking-command-handler

# With coverage
mvn clean verify
```

5. **Skip tests (faster build):**
```bash
mvn clean install -DskipTests
```

---

## How to Run

### Option 1: Using Maven (Development)

**Step 1: Start Infrastructure**
```bash
cd .docker
docker-compose up -d postgres mongo kafka redis
```

**Step 2: Start Command Handler**
```bash
cd booking-command-handler
mvn spring-boot:run
```

**Step 3: Start Query Handler (in another terminal)**
```bash
cd booking-query-handler
mvn spring-boot:run
```

### Option 2: Using JAR (Production-like)

**Step 1: Build JARs**
```bash
mvn clean package
```

**Step 2: Start Infrastructure**
```bash
cd .docker
docker-compose up -d postgres mongo kafka redis
```

**Step 3: Run Command Handler**
```bash
java -jar booking-command-handler/target/booking-command-handler-exec.jar
```

**Step 4: Run Query Handler**
```bash
java -jar booking-query-handler/target/booking-query-handler-exec.jar
```

### Option 3: Using Docker Compose (Full Stack)

```bash
cd .docker
docker-compose up -d
```

This starts all infrastructure components:
- PostgreSQL (Event Store) - Port 5432
- MongoDB (Projections) - Port 27017
- Mongo Express (UI) - Port 8081
- Apache Kafka - Port 9092
- Kafbat UI (Kafka Management) - Port 18080
- Redis - Port 6379
- SonarQube - Port 9000

### Verify the Service

**Command Handler Health Check:**
```bash
curl http://localhost:8080/actuator/health
```

**Query Handler Health Check:**
```bash
curl http://localhost:8081/actuator/health
```

**Create a Booking:**
```bash
curl -X POST http://localhost:8080/command-handler/api/bookings/create \
  -H "Accept-Version: application/vnd.booking-command-handler.v1+json" \
  -H "Content-Type: application/json" \
  -d '{
    "paxes": [{
      "pax_id": "123e4567-e89b-12d3-a456-426614174000",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john@example.com",
      "phone": "+1234567890",
      "age": 30,
      "document_type": "PASSPORT",
      "document_number": "AB123456",
      "pax_type": "ADULT"
    }],
    "lead_pax_id": "123e4567-e89b-12d3-a456-426614174000",
    "products": [{
      "product_id": "prod-001",
      "product_name": "Flight Ticket",
      "product_type": "FLIGHT",
      "price": 299.99,
      "currency": "USD"
    }],
    "metadata": {
      "source": "web",
      "channel": "direct"
    }
  }'
```

**Query a Booking:**
```bash
curl http://localhost:8081/query-handler/api/bookings/{bookingId} \
  -H "Accept-Version: application/vnd.booking-query-handler.v1+json"
```

---

## How to Check SonarQube

### 1. Start SonarQube

```bash
cd .docker
docker-compose up -d sonarqube sonarqube-db
```

Wait for SonarQube to start (check logs):
```bash
docker logs -f sonarqube-local
```

### 2. Login to SonarQube

Open browser: http://localhost:9000

Default credentials:
- Username: `admin`
- Password: `admin`

(You'll be prompted to change the password on first login)

### 3. Generate SonarQube Token

1. Go to **My Account** → **Security** → **Generate Tokens**
2. Name: `booking-service-analysis`
3. Type: `Project Analysis Token`
4. Click **Generate**
5. Copy the token (you won't see it again)

### 4. Run Maven Sonar Analysis

```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=booking-service \
  -Dsonar.projectName='Booking Service' \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<YOUR_TOKEN_HERE>
```

### 5. View Results

Open http://localhost:9000/dashboard?id=booking-service

### SonarQube Configuration

Add to `pom.xml` (already configured):
```xml
<properties>
    <sonar.organization>your-org</sonar.organization>
    <sonar.host.url>http://localhost:9000</sonar.host.url>
    <sonar.coverage.jacoco.xmlReportPaths>
        ${project.build.directory}/site/jacoco/jacoco.xml
    </sonar.coverage.jacoco.xmlReportPaths>
</properties>
```

---

## API Endpoints

### Command Handler (Port 8080)

**Base URL:** `http://localhost:8080/command-handler/api/bookings`

**Header Required:** `Accept-Version: application/vnd.booking-command-handler.v1+json`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/create` | Create a new booking |
| POST | `/place` | Place a booking |
| POST | `/{bookingId}/confirm` | Confirm a booking |
| POST | `/{bookingId}/cancel` | Cancel a booking |
| POST | `/{bookingId}/complete` | Complete a booking |
| POST | `/{bookingId}/expire` | Expire a booking |

### Query Handler (Port 8081)

**Base URL:** `http://localhost:8081/query-handler/api/bookings`

**Header Required:** `Accept-Version: application/vnd.booking-query-handler.v1+json`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/{bookingId}` | Get booking by ID |
| GET | `/` | List all bookings (with pagination) |
| GET | `/search` | Search bookings by criteria |

---

## Additional Resources

### Documentation Files

- [Projection Synchronization Guide](PROJECTION_SYNCHRONIZATION_GUIDE.md) - **Important: Read this first!**
- [Command Handler README](booking-command-handler/README.md)
- [Query Handler README](booking-query-handler/README.md)
- [Antic AI Integration Architecture](AGENTIC_AI_BOOKING_SERVICE.md)

### API Documentation

- **Command Handler Swagger UI**: http://localhost:8080/swagger-ui.html
- **Query Handler Swagger UI**: http://localhost:8081/swagger-ui.html
- **Command Handler OpenAPI**: http://localhost:8080/v3/api-docs
- **Query Handler OpenAPI**: http://localhost:8081/v3/api-docs

### Monitoring & Management

- **Command Handler Actuator**: http://localhost:8080/actuator
- **Query Handler Actuator**: http://localhost:8081/actuator
- **Mongo Express**: http://localhost:8081 (MongoDB UI)
- **Kafbat UI**: http://localhost:18080 (Kafka Management)
- **SonarQube**: http://localhost:9000

### External Links

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Apache Kafka](https://kafka.apache.org/documentation/)

---

## License

This project is licensed under the terms specified in the LICENSE file.

---

## Contact & Support

For questions, issues, or contributions, please refer to the project repository.

**Project Statistics:**
- Total Lines of Code: ~15,000+
- Test Coverage: 141 unit tests (100% pass rate)
- Modules: 2 microservices + 1 core library
- API Endpoints: 12+ REST endpoints
- Supported Operations: 6 command types, 6 event types
