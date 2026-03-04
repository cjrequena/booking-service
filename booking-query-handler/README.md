# Booking Query Handler - Read Side Microservice

## Abstract

The Booking Query Handler is the read-side microservice in the CQRS architecture, responsible for serving all booking queries with optimized read performance. It maintains denormalized projections in MongoDB and PostgreSQL that are synchronized by polling the Event Store, enabling fast and efficient query operations without impacting the write side.

This service polls the Event Store for new events, processes them to update projections, and provides RESTful query endpoints with support for filtering, pagination, and sorting.

---

## Overview

The Query Handler implements the read model in a CQRS architecture, focusing on query optimization and read performance. It maintains eventually consistent projections that are optimized for specific query patterns.

### Key Features

- **Optimized Read Model**: Denormalized MongoDB and PostgreSQL projections for fast queries
- **Event Store Polling**: ScheduledEventHandlerService polls Event Store for new events
- **Event-Driven Synchronization**: Processes events to update projections
- **Reactive Programming**: Non-blocking operations using Spring WebFlux
- **RESTful Query API**: Well-documented REST endpoints with OpenAPI/Swagger
- **Flexible Querying**: Support for filtering, pagination, and sorting
- **Eventual Consistency**: Projections updated asynchronously from Event Store
- **Caching**: Multi-level caching strategy for improved performance
- **Reactive MongoDB**: Non-blocking database operations
- **Health Monitoring**: Actuator endpoints for health checks and metrics
- **API Versioning**: Header-based versioning for backward compatibility
- **Configurable Polling**: Adjustable polling intervals and batch sizes

### Technology Stack

**Core:**
- Java 21
- Spring Boot 3.4.1
- Spring WebFlux (Reactive)
- Spring Data MongoDB Reactive

**Database:**
- MongoDB 8 (Projections)
- PostgreSQL 17 (Optional Projections)
- Reactive MongoDB Driver

**Event Store Access:**
- PostgreSQL 17 (Event Store - Read Only)
- Scheduled Polling Service

**Messaging (Optional):**
- Apache Kafka (for external event publishing only)
- Spring Cloud Stream

**Caching:**
- Caffeine (Local Cache)
- Spring Cache Abstraction

**Development:**
- Lombok
- MapStruct
- JUnit 5
- Mockito

---

## Architecture

### Component Structure

```
booking-query-handler/
├── controller/              # REST API Layer
│   ├── BookingQueryController
│   ├── dto/                # Data Transfer Objects
│   └── exception/          # Exception Handling
├── service/                # Application Service Layer
│   ├── query/             # Query Services
│   └── projection/        # Projection Services
├── domain/                 # Domain Layer
│   ├── model/
│   │   ├── entity/        # MongoDB Entities
│   │   └── event/         # Domain Events (for consumption)
│   ├── mapper/            # Domain Mappers
│   └── exception/         # Domain Exceptions
├── repository/             # Data Access Layer
│   └── BookingRepository  # Reactive MongoDB Repository
├── consumer/              # Event Consumers
│   └── BookingEventConsumer
├── configuration/          # Spring Configuration
└── shared/                # Shared Utilities
```

### Request Flow

**Query Flow:**
```
1. Client Request
   ↓
2. BookingQueryController
   ↓
3. Query Validation
   ↓
4. BookingQueryService
   ↓
5. BookingRepository (MongoDB Reactive)
   ↓
6. Cache Check (Caffeine)
   ↓
7. MongoDB Query
   ↓
8. Response Mapping
   ↓
9. Response to Client
```

**Event Processing Flow (when projections.handlers.enabled=true):**
```
1. ScheduledEventHandlerService (Polls Event Store)
   ↓
2. Fetch New Events from PostgreSQL Event Store
   ↓
3. BookingEventHandler
   ↓
4. Event Deserialization
   ↓
5. BookingProjectionService
   ↓
6. Projection Update Logic
   ↓
7. BookingRepository (MongoDB)
   ↓
8. PostgreSQL Projection Repository (Optional)
   ↓
9. Cache Invalidation
   ↓
10. Projection Updated
```

### Projection Model

**Booking Projection Entity:**
```java
@Document(collection = "bookings")
public class BookingEntity {
    @Id
    private String id;
    private UUID bookingId;
    private String bookingReference;
    private BookingStatus status;
    private List<PaxEntity> paxes;
    private UUID leadPaxId;
    private List<ProductEntity> products;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
```

**Projection Synchronization:**
- Events polled from PostgreSQL Event Store
- ScheduledEventHandlerService runs at configured intervals
- Projections updated in real-time or near real-time
- Eventual consistency model
- Idempotent event processing
- Configurable batch size and polling frequency

### Key Components

**1. Booking Query Service**
- Retrieves bookings by ID
- Lists bookings with pagination
- Searches bookings by criteria
- Applies caching strategies

**2. Booking Projection Service**
- Processes domain events from Event Store
- Updates MongoDB projections
- Updates PostgreSQL projections (optional)
- Handles event ordering
- Ensures idempotency

**3. Scheduled Event Handler Service**
- Polls PostgreSQL Event Store for new events
- Configurable polling interval
- Batch processing support
- Tracks last processed event
- Error handling and retry logic

**4. Booking Repository**
- Reactive MongoDB operations
- Custom query methods
- Pagination support
- Sorting capabilities

---

## How to Compile

### Prerequisites

- Java 21+
- Maven 3.9+
- MongoDB 8 (for projections)
- PostgreSQL 17 (for Event Store access - read only)

### Compilation Steps

```bash
# Navigate to query handler directory
cd booking-query-handler

# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package (creates executable JAR)
mvn clean package

# Skip tests for faster build
mvn clean package -DskipTests

# Install to local Maven repository
mvn clean install
```

### Build Artifacts

After successful build:
- `target/booking-query-handler.jar` - Standard JAR
- `target/booking-query-handler-exec.jar` - Executable JAR with dependencies

---

## How to Run

### Option 1: Using Maven (Development)

**Step 1: Start Infrastructure**
```bash
cd ../.docker
docker-compose up -d postgres mongo
```

**Step 2: Run Application**
```bash
cd booking-query-handler
mvn spring-boot:run
```

**Step 3: Verify**
```bash
curl http://localhost:8081/actuator/health
```

### Option 2: Using JAR (Production-like)

**Step 1: Build JAR**
```bash
mvn clean package
```

**Step 2: Run JAR**
```bash
java -jar target/booking-query-handler-exec.jar
```

**With Custom Configuration:**
```bash
java -jar target/booking-query-handler-exec.jar \
  --spring.profiles.active=prod \
  --server.port=8081 \
  --spring.data.mongodb.uri=mongodb://localhost:27017/bookings
```

### Option 3: Using Docker

**Build Docker Image:**
```bash
docker build -t booking-query-handler:latest .
```

**Run Container:**
```bash
docker run -p 8081:8081 \
  -e SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/bookings \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/eventstore \
  booking-query-handler:latest
```

### Configuration

**application.yml:**
```yaml
server:
  port: 8081

spring:
  application:
    name: booking-query-handler
  
  # MongoDB for projections
  data:
    mongodb:
      uri: mongodb://localhost:27017/bookings
      auto-index-creation: true
  
  # PostgreSQL Event Store (read-only access)
  datasource:
    url: jdbc:postgresql://localhost:5432/eventstore
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 5
      read-only: true

  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m

# Projection configuration
projections:
  handlers:
    enabled: true  # Enable to poll Event Store and update projections

# Event polling configuration
event-store:
  polling:
    enabled: true
    interval: 5000  # Poll every 5 seconds
    batch-size: 100  # Process 100 events per batch

logging:
  level:
    com.cjrequena.sample.query.handler: DEBUG
    org.springframework.data.mongodb: DEBUG
```

### Verify the Service

**Health Check:**
```bash
curl http://localhost:8081/actuator/health
```

**Swagger UI:**
```
http://localhost:8081/swagger-ui.html
```

**Query Booking Example:**
```bash
curl http://localhost:8081/query-handler/api/bookings/{bookingId} \
  -H "Accept-Version: application/vnd.booking-query-handler.v1+json"
```

**List Bookings Example:**
```bash
curl "http://localhost:8081/query-handler/api/bookings?page=0&size=10&sort=createdAt,desc" \
  -H "Accept-Version: application/vnd.booking-query-handler.v1+json"
```

---

## API Endpoints

### Base URL
`http://localhost:8081/query-handler/api/bookings`

### Required Header
`Accept-Version: application/vnd.booking-query-handler.v1+json`

### Endpoints

#### 1. Get Booking by ID
```http
GET /{bookingId}

Response: 200 OK
{
  "booking_id": "uuid",
  "booking_reference": "BK-2026-001",
  "status": "CONFIRMED",
  "paxes": [
    {
      "pax_id": "uuid",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john@example.com",
      "phone": "+1234567890",
      "age": 30,
      "document_type": "PASSPORT",
      "document_number": "AB123456",
      "pax_type": "ADULT"
    }
  ],
  "lead_pax_id": "uuid",
  "products": [
    {
      "product_id": "prod-001",
      "product_name": "Flight Ticket",
      "product_type": "FLIGHT",
      "price": 299.99,
      "currency": "USD"
    }
  ],
  "metadata": {
    "source": "web",
    "channel": "direct"
  },
  "created_at": "2026-03-04T10:30:00Z",
  "updated_at": "2026-03-04T11:00:00Z",
  "version": 2
}
```

#### 2. List All Bookings
```http
GET /?page=0&size=10&sort=createdAt,desc

Response: 200 OK
{
  "content": [
    {
      "booking_id": "uuid",
      "booking_reference": "BK-2026-001",
      "status": "CONFIRMED",
      ...
    }
  ],
  "page": {
    "size": 10,
    "number": 0,
    "total_elements": 100,
    "total_pages": 10
  }
}
```

#### 3. Search Bookings
```http
GET /search?status=CONFIRMED&leadPaxEmail=john@example.com

Response: 200 OK
{
  "content": [
    {
      "booking_id": "uuid",
      "booking_reference": "BK-2026-001",
      "status": "CONFIRMED",
      ...
    }
  ]
}
```

#### 4. Get Booking by Reference
```http
GET /reference/{bookingReference}

Response: 200 OK
{
  "booking_id": "uuid",
  "booking_reference": "BK-2026-001",
  ...
}
```

### Query Parameters

**Pagination:**
- `page` - Page number (default: 0)
- `size` - Page size (default: 20, max: 100)
- `sort` - Sort field and direction (e.g., `createdAt,desc`)

**Filtering:**
- `status` - Filter by booking status
- `leadPaxEmail` - Filter by lead passenger email
- `createdAfter` - Filter by creation date (ISO 8601)
- `createdBefore` - Filter by creation date (ISO 8601)

### Error Responses

```json
{
  "timestamp": "2026-03-04T15:30:00",
  "status": 404,
  "error_code": "Not Found",
  "message": "Booking not found with ID: 123e4567-e89b-12d3-a456-426614174000"
}
```

**HTTP Status Codes:**
- `200 OK` - Successful query
- `400 Bad Request` - Invalid query parameters
- `404 Not Found` - Booking not found
- `500 Internal Server Error` - Unexpected error

---

## Event Store Polling

### Polling Configuration

The Query Handler polls the PostgreSQL Event Store to fetch new events and update projections.

**Configuration:**
```yaml
projections:
  handlers:
    enabled: true  # Enable projection updates

event-store:
  polling:
    enabled: true
    interval: 5000      # Poll every 5 seconds
    batch-size: 100     # Process 100 events per batch
    initial-delay: 10000 # Wait 10 seconds before first poll
```

### Scheduled Event Handler Service

**Implementation:**
```java
@Service
@EnableScheduling
public class ScheduledEventHandlerService {
    
    @Scheduled(fixedDelayString = "${event-store.polling.interval:5000}")
    public void pollAndProcessEvents() {
        // 1. Fetch last processed event ID
        Long lastProcessedEventId = getLastProcessedEventId();
        
        // 2. Query Event Store for new events
        List<EventEntity> newEvents = eventStoreRepository
            .findByIdGreaterThanOrderByIdAsc(
                lastProcessedEventId, 
                PageRequest.of(0, batchSize)
            );
        
        // 3. Process each event
        newEvents.forEach(event -> {
            bookingEventHandler.handle(event);
            updateLastProcessedEventId(event.getId());
        });
    }
}
```

### Event Processing

**Event Handler:**
```java
@Component
public class BookingEventHandler {
    
    public void handle(EventEntity eventEntity) {
        Event event = eventMapper.toEvent(eventEntity);
        
        switch (event.getEventType()) {
            case "BookingCreatedEvent":
                projectionService.handleBookingCreated((BookingCreatedEvent) event);
                break;
            case "BookingPlacedEvent":
                projectionService.handleBookingPlaced((BookingPlacedEvent) event);
                break;
            case "BookingConfirmedEvent":
                projectionService.handleBookingConfirmed((BookingConfirmedEvent) event);
                break;
            case "BookingCancelledEvent":
                projectionService.handleBookingCancelled((BookingCancelledEvent) event);
                break;
            case "BookingCompletedEvent":
                projectionService.handleBookingCompleted((BookingCompletedEvent) event);
                break;
            case "BookingExpiredEvent":
                projectionService.handleBookingExpired((BookingExpiredEvent) event);
                break;
        }
    }
}
```

**Projection Update:**
```java
@Service
public class BookingProjectionService {
    
    public void handleBookingCreated(BookingCreatedEvent event) {
        BookingEntity entity = new BookingEntity();
        entity.setBookingId(event.getAggregateId());
        entity.setStatus(BookingStatus.CREATED);
        entity.setPaxes(mapPaxes(event.getData().paxes()));
        entity.setProducts(mapProducts(event.getData().products()));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setVersion(event.getAggregateVersion());
        
        repository.save(entity).subscribe();
    }
    
    public void handleBookingPlaced(BookingPlacedEvent event) {
        repository.findByBookingId(event.getAggregateId())
            .flatMap(entity -> {
                entity.setStatus(BookingStatus.PLACED);
                entity.setUpdatedAt(LocalDateTime.now());
                entity.setVersion(event.getAggregateVersion());
                return repository.save(entity);
            })
            .subscribe();
    }
    
    // ... other event handlers
}
```

### Error Handling

- **Retry Strategy**: Automatic retry with exponential backoff
- **Dead Letter Storage**: Failed events logged for manual review
- **Idempotency**: Events processed idempotently using event ID and version
- **Monitoring**: Event processing metrics exposed via Actuator
- **Circuit Breaker**: Stops polling if too many consecutive failures

### Monitoring

**Metrics Available:**
- `event.store.polling.last.processed.id` - Last processed event ID
- `event.store.polling.batch.size` - Number of events in last batch
- `event.store.polling.processing.time` - Time to process last batch
- `event.store.polling.errors` - Number of processing errors

**Health Check:**
```bash
curl http://localhost:8081/actuator/health/eventStorePolling
```

---

## Event Consumption (Deprecated - Kafka)

### Kafka Configuration (Optional - For External Systems Only)

**Note:** Kafka is NOT used for internal projection synchronization. It's only used if you want to publish events to external systems.

**Topic:** `booking-events` (external)

**Event Types Published:**
- `BookingCreatedEvent`
- `BookingPlacedEvent`
- `BookingConfirmedEvent`
- `BookingCancelledEvent`
- `BookingCompletedEvent`
- `BookingExpiredEvent`

**Note:** This Kafka configuration is deprecated for internal projection synchronization. The Query Handler now polls the Event Store directly. Kafka is only used for publishing events to external systems.

---

## Caching Strategy

### Cache Configuration

**Cache Provider:** Caffeine

**Cache Specifications:**
- Maximum Size: 1000 entries
- Expiration: 10 minutes after write
- Eviction Policy: LRU (Least Recently Used)

### Cached Operations

```java
@Cacheable(value = "bookings", key = "#bookingId")
public Mono<BookingDTO> findByBookingId(UUID bookingId) {
    return repository.findByBookingId(bookingId)
        .map(mapper::toDTO);
}

@CacheEvict(value = "bookings", key = "#bookingId")
public void evictCache(UUID bookingId) {
    // Cache evicted on projection update
}
```

### Cache Invalidation

- Automatic invalidation on projection updates
- Manual invalidation via Actuator endpoint
- TTL-based expiration

---

## Testing

### Test Coverage

Tests are primarily focused on the command handler. Query handler tests would include:

**Recommended Test Structure:**
```
src/test/java/
├── controller/
│   └── BookingQueryControllerTest.java
├── service/
│   ├── BookingQueryServiceTest.java
│   └── BookingProjectionServiceTest.java
├── repository/
│   └── BookingRepositoryTest.java
└── consumer/
    └── BookingEventConsumerTest.java
```

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=BookingQueryServiceTest

# With coverage report
mvn clean verify

# View coverage report
open target/site/jacoco/index.html
```

---

## Additional Resources

### API Documentation

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8081/v3/api-docs
- **OpenAPI YAML**: http://localhost:8081/v3/api-docs.yaml

### Monitoring & Management

- **Health**: http://localhost:8081/actuator/health
- **Info**: http://localhost:8081/actuator/info
- **Metrics**: http://localhost:8081/actuator/metrics
- **Prometheus**: http://localhost:8081/actuator/prometheus
- **Cache Stats**: http://localhost:8081/actuator/caches

### MongoDB Collections

**Bookings Collection:**
```javascript
db.bookings.createIndex({ "booking_id": 1 }, { unique: true })
db.bookings.createIndex({ "booking_reference": 1 }, { unique: true })
db.bookings.createIndex({ "status": 1 })
db.bookings.createIndex({ "lead_pax_id": 1 })
db.bookings.createIndex({ "created_at": -1 })
db.bookings.createIndex({ "paxes.email": 1 })
```

**Sample Document:**
```json
{
  "_id": ObjectId("..."),
  "booking_id": "123e4567-e89b-12d3-a456-426614174000",
  "booking_reference": "BK-2026-001",
  "status": "CONFIRMED",
  "paxes": [
    {
      "pax_id": "...",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john@example.com",
      ...
    }
  ],
  "lead_pax_id": "...",
  "products": [...],
  "metadata": {...},
  "created_at": ISODate("2026-03-04T10:30:00Z"),
  "updated_at": ISODate("2026-03-04T11:00:00Z"),
  "version": 2
}
```

### External Links

- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Event-Driven Architecture](https://martinfowler.com/articles/201701-event-driven.html)
- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Reactive MongoDB](https://docs.spring.io/spring-data/mongodb/reference/mongodb/reactive-mongodb.html)
- [Apache Kafka](https://kafka.apache.org/documentation/)

---

## License

This project is licensed under the terms specified in the LICENSE file.

---

## Contact & Support

For questions, issues, or contributions, please refer to the project repository.

**Module Statistics:**
- Lines of Code: ~5,000+
- API Endpoints: 4+ query endpoints
- Event Types: 6 event types consumed
- Collections: 1 (bookings)
- Indexes: 6 MongoDB indexes
