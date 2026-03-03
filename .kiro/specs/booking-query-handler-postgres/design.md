# Design Document: PostgreSQL Projection for Booking Query Handler

## Overview

This design document specifies the implementation of a PostgreSQL-based projection in a new `booking-query-handler-postgres` module. This is a separate Spring Boot application from the existing `booking-query-handler` module (which uses MongoDB). The implementation follows the Event Store Subscription approach (Approach 1 from the implementation guide), which provides clean CQRS separation by moving event handling logic to a dedicated query-handler while maintaining snapshot optimization for efficient projection rebuilding.

**IMPORTANT: All code for this feature must be implemented in the `booking-query-handler-postgres` module.**

**Module Location:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/`

**Base Package:** `com.cjrequena.sample.query.handler.postgres`

**Self-Contained Module:** The `booking-query-handler-postgres` module is completely self-contained with its own domain models, enums, aggregates, and exceptions. It does NOT depend on domain classes from `booking-command-handler` or `booking-query-handler` modules. The only shared dependency is the `es-core` framework for Event Sourcing infrastructure (Aggregate base class, Event, etc.).

**Domain Model Strategy:** The postgres module contains its own copy of the Booking aggregate and all value objects (PaxVO, ProductVO, TransferVO, etc.) in the `com.cjrequena.sample.query.handler.postgres.domain` package. These mirror the command-handler's domain model structure to ensure compatibility when replaying events, but they are independent copies that live in the postgres module's package structure.

**✅ Implementation Status - Domain Models (Task 7.2 Completed):**

All domain models have been successfully implemented in the postgres module with correct package structure:

- **Package:** `com.cjrequena.sample.query.handler.postgres.domain.*`
- **Enums:** AggregateType, BookingStatus, ProductStatus, ProductType, TransferServiceType, TransferType
- **Exceptions:** DomainRuntimeException, InvalidArgumentException, PaxPriceException  
- **Value Objects:** PaxVO, PaxPriceVO, LocationVO, ProductMetadataVO, ProductVO (interface), VehicleVO, TripVO, TransferPriceVO, TransferVO
- **Aggregate:** Booking (simplified for query-side with event replay support)
- **Mapper:** BookingEntityMapper correctly uses local postgres domain models (no command-handler dependencies)

The system architecture separates command and query responsibilities:

- **Command-Handler** (`booking-command-handler` module): Processes commands, updates aggregates, persists events to Event Store
- **Query-Handler MongoDB** (`booking-query-handler` module): Existing module with MongoDB projection (unchanged)
- **Query-Handler PostgreSQL** (`booking-query-handler-postgres` module): NEW module with PostgreSQL projection (this spec)
- **ES-Core** (`es-core` module): Shared Event Sourcing framework

This architecture enables:
- True CQRS separation with no projection logic in the command side
- Independent deployment and scaling of each query handler
- Easy addition of new projections without modifying existing modules
- Efficient projection rebuilding through snapshot optimization
- Independent scaling of read and write operations

The PostgreSQL projection uses a hybrid schema design that balances relational integrity with flexibility for polymorphic data, combining normalized tables for core entities with JSONB columns for product-specific details.


## Architecture

### High-Level Architecture

The system follows the Event Store Subscription pattern where the new `booking-query-handler-postgres` module polls the Event Store for new events and updates the PostgreSQL projection:

```
┌─────────────────────────────────────────────────────────────┐
│                   Command-Handler                            │
│                   (booking-command-handler module)           │
├─────────────────────────────────────────────────────────────┤
│  1. Receive command                                         │
│  2. Update aggregate                                        │
│  3. Persist events to Event Store                           │
│  4. Done! (No projection logic)                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  Event Store  │
                    │  (PostgreSQL) │
                    └───────────────┘
                      │           │
        ┌─────────────┘           └─────────────┐
        ▼                                       ▼
┌──────────────────┐                  ┌──────────────────────┐
│  Query-Handler   │                  │ Query-Handler-Postgres│
│  (MongoDB)       │                  │ (NEW MODULE)          │
│  UNCHANGED       │                  │ THIS SPEC             │
└──────────────────┘                  └──────────────────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │   PostgreSQL    │
                              │   Projection    │
                              └─────────────────┘
```

### Component Architecture

```
Query-Handler-Postgres Module (NEW)
├── Event Subscription Layer
│   ├── ScheduledEventHandlerService (polls Event Store)
│   └── BookingEventHandler (processes events)
│
├── Projection Layer
│   └── BookingPostgresProjectionHandler
│
├── PostgreSQL Persistence Layer
│   ├── Entities (BookingEntity, PaxEntity, ProductEntity)
│   ├── Repositories (BookingPostgresRepository)
│   ├── Services (BookingPostgresProjectionService)
│   └── Mappers (BookingEntityMapper)
│
└── API Layer
    └── BookingQueryController (REST endpoints)

Package: com.cjrequena.sample.query.handler.postgres
```

### Event Processing Flow

```
1. Event Store Polling
   ScheduledEventHandlerService (in booking-query-handler-postgres)
   └─> Polls Event Store every 1 second
   └─> Retrieves new events after last processed offset

2. Event Processing
   BookingEventHandler (in booking-query-handler-postgres)
   └─> Groups events by aggregate ID
   └─> For each unique aggregate:
       ├─> Rebuild aggregate from Event Store
       │   ├─> Check for snapshot (if enabled)
       │   ├─> Load snapshot (if available)
       │   └─> Replay events after snapshot
       │
       └─> Update PostgreSQL projection
           └─> BookingPostgresProjectionHandler
               └─> Convert aggregate to PostgreSQL entity
               └─> Save to PostgreSQL

3. Offset Management
   └─> If projection succeeds: Update subscription offset
   └─> If projection fails: Propagate exception, retry on next poll
```

### Snapshot Optimization

The system leverages snapshot optimization to minimize event processing:

```
Without Snapshot:
Aggregate with 10,000 events → Process all 10,000 events

With Snapshot (at version 9,500):
Aggregate with 10,000 events → Load snapshot + Process 500 events
```

This optimization is critical for:
- Fast projection rebuilding when adding new projections
- Efficient recovery from projection corruption
- Reduced load on the Event Store

## Components and Interfaces

### Projection Handler Interface

The `ProjectionHandler` interface provides a unified contract for all projection implementations:

```java
public interface ProjectionHandler {
    /**
     * Updates the projection with the given aggregate.
     * @param aggregate the aggregate to project
     */
    void handle(Aggregate aggregate);
    
    /**
     * Returns the aggregate type this handler processes.
     * @return the aggregate type
     */
    AggregateType getAggregateType();
}
```

### Event Handler

The `BookingEventHandler` orchestrates event processing and projection updates:

```java
@Component
@Transactional
public class BookingEventHandler extends EventHandler {
    private final List<ProjectionHandler> projectionHandlers;
    
    @Override
    public void handle(List<EventEntity> eventEntityList) {
        // Convert event entities to domain events
        final List<Event> events = eventMapper.toEventList(eventEntityList);
        
        // Process each unique aggregate in parallel
        events.parallelStream()
            .map(Event::getAggregateId)
            .distinct()
            .forEach(aggregateId -> {
                // Rebuild aggregate from Event Store (with snapshot optimization)
                final Aggregate aggregate = retrieveOrInstantiateAggregate(aggregateId);
                
                // Update all registered projections
                projectionHandlers.stream()
                    .filter(handler -> handler.getAggregateType().getType()
                        .equals(aggregate.getAggregateType()))
                    .forEach(handler -> handler.handle(aggregate));
            });
    }
}
```

### MongoDB Projection Handler

Maintains the existing MongoDB projection:

```java
@Component
public class BookingMongoProjectionHandler implements ProjectionHandler {
    private final BookingMongoProjectionService mongoService;
    
    @Override
    @Transactional
    public void handle(Aggregate aggregate) {
        Booking booking = (Booking) aggregate;
        mongoService.save(booking);
    }
    
    @Override
    public AggregateType getAggregateType() {
        return AggregateType.BOOKING_ORDER;
    }
}
```

### PostgreSQL Projection Handler

Implements the new PostgreSQL projection:

```java
@Component
public class BookingPostgresProjectionHandler implements ProjectionHandler {
    private final BookingPostgresProjectionService postgresService;
    private final BookingEntityMapper mapper;
    
    @Override
    @Transactional
    public void handle(Aggregate aggregate) {
        Booking booking = (Booking) aggregate;
        
        // Convert aggregate to PostgreSQL entity
        BookingEntity entity = mapper.toEntity(booking);
        
        // Save to PostgreSQL
        postgresService.save(entity);
    }
    
    @Override
    public AggregateType getAggregateType() {
        return AggregateType.BOOKING_ORDER;
    }
}
```

### Repository Layer

The repository provides query methods for the PostgreSQL projection:

```java
@Repository
public interface BookingPostgresRepository extends JpaRepository<BookingEntity, UUID> {
    Optional<BookingEntity> findByBookingReference(String bookingReference);
    List<BookingEntity> findByStatus(BookingStatus status);
    List<BookingEntity> findByLeadPaxId(UUID leadPaxId);
    
    @Query("SELECT b FROM BookingEntity b JOIN b.paxes p WHERE p.email = :email")
    List<BookingEntity> findByPaxEmail(@Param("email") String email);
    
    @Query("SELECT b FROM BookingEntity b " +
           "LEFT JOIN FETCH b.paxes " +
           "LEFT JOIN FETCH b.products " +
           "WHERE b.bookingId = :bookingId")
    Optional<BookingEntity> findByIdWithDetails(@Param("bookingId") UUID bookingId);
}
```

### Service Layer

The service layer provides business logic for querying the PostgreSQL projection:

```java
@Service
@Transactional(readOnly = true)
public class BookingPostgresProjectionService {
    private final BookingPostgresRepository repository;
    
    @Cacheable(value = "postgres-bookings", key = "#bookingId")
    public BookingEntity retrieveById(UUID bookingId) {
        return repository.findByIdWithDetails(bookingId)
            .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));
    }
    
    public BookingEntity retrieveByReference(String bookingReference) {
        return repository.findByBookingReference(bookingReference)
            .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingReference));
    }
    
    public List<BookingEntity> retrieveAll() {
        return repository.findAll();
    }
    
    @Transactional
    public void save(BookingEntity entity) {
        repository.save(entity);
    }
}
```

### Mapper Component

The mapper converts domain aggregates to PostgreSQL entities:

```java
package com.cjrequena.sample.query.handler.postgres.mapper;

import com.cjrequena.sample.query.handler.postgres.domain.aggregate.Booking;
import com.cjrequena.sample.query.handler.postgres.domain.vo.PaxVO;
import com.cjrequena.sample.query.handler.postgres.domain.vo.ProductVO;
import com.cjrequena.sample.query.handler.postgres.domain.vo.transfer.TransferVO;
import com.cjrequena.sample.query.handler.postgres.persistence.entity.BookingEntity;
import com.cjrequena.sample.query.handler.postgres.persistence.entity.PaxEntity;
import com.cjrequena.sample.query.handler.postgres.persistence.entity.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class BookingEntityMapper {
    public BookingEntity toEntity(Booking booking) {
        BookingEntity entity = BookingEntity.builder()
            .bookingId(booking.getBookingId())
            .bookingReference(booking.getBookingReference())
            .status(booking.getStatus())
            .leadPaxId(booking.getLeadPaxId())
            .build();
        
        // Map passengers
        booking.getPaxes().forEach(pax -> {
            PaxEntity paxEntity = toPaxEntity(pax);
            entity.addPax(paxEntity);
        });
        
        // Map products
        booking.getProducts().forEach(product -> {
            ProductEntity productEntity = toProductEntity(product);
            entity.addProduct(productEntity);
        });
        
        return entity;
    }
    
    private PaxEntity toPaxEntity(Pax pax) {
        return PaxEntity.builder()
            .paxId(pax.getPaxId())
            .firstName(pax.getFirstName())
            .lastName(pax.getLastName())
            .email(pax.getEmail())
            .phone(pax.getPhone())
            .age(pax.getAge())
            .documentType(pax.getDocumentType())
            .documentNumber(pax.getDocumentNumber())
            .paxType(pax.getPaxType())
            .build();
    }
    
    private ProductEntity toProductEntity(Product product) {
        ProductEntity entity = ProductEntity.builder()
            .productId(product.getProductId())
            .searchId(product.getSearchId())
            .searchCreatedAt(product.getSearchCreatedAt())
            .productType(product.getProductType())
            .status(product.getStatus())
            .hash(product.getHash())
            .paxesIds(product.getPaxesIds())
            .build();
        
        // Serialize product-specific details to JSONB
        if (product instanceof Transfer) {
            entity.setProductDetails(serializeTransfer((Transfer) product));
        }
        // Future: Handle Activity, Hotel, etc.
        
        return entity;
    }
    
    private Map<String, Object> serializeTransfer(Transfer transfer) {
        // Serialize transfer-specific fields to Map for JSONB storage
        // Includes origin, destination, outbound_trip, inbound_trip, price
        // Implementation details in mapper class
    }
}
```

## Data Models

### Database Schema Design

The PostgreSQL projection uses a hybrid schema that combines normalized relational tables with JSONB columns for polymorphic data:

#### Booking Table

```sql
CREATE TABLE booking (
    booking_id UUID PRIMARY KEY,
    booking_reference VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,
    lead_pax_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_booking_reference ON booking(booking_reference);
CREATE INDEX idx_booking_status ON booking(status);
CREATE INDEX idx_booking_lead_pax ON booking(lead_pax_id);
```

#### Booking Pax Table (Normalized)

```sql
CREATE TABLE booking_pax (
    pax_id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    age INTEGER NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    pax_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_booking_pax_booking 
        FOREIGN KEY (booking_id) 
        REFERENCES booking(booking_id) 
        ON DELETE CASCADE
);

CREATE INDEX idx_booking_pax_email ON booking_pax(email);
CREATE INDEX idx_booking_pax_booking_id ON booking_pax(booking_id);
```

#### Booking Product Table (Hybrid with JSONB)

```sql
CREATE TABLE booking_product (
    product_id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    search_id UUID NOT NULL,
    search_created_at TIMESTAMP NOT NULL,
    product_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    hash VARCHAR(255) NOT NULL,
    paxes_ids JSONB NOT NULL,
    product_details JSONB NOT NULL,
    CONSTRAINT fk_booking_product_booking 
        FOREIGN KEY (booking_id) 
        REFERENCES booking(booking_id) 
        ON DELETE CASCADE
);

CREATE INDEX idx_booking_product_booking_id ON booking_product(booking_id);
CREATE INDEX idx_booking_product_type ON booking_product(product_type);
CREATE INDEX idx_booking_product_details ON booking_product USING GIN (product_details);
```

### JPA Entity Models

#### BookingEntity

```java
@Entity
@Table(name = "booking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEntity {
    @Id
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;
    
    @Column(name = "booking_reference", nullable = false, unique = true, length = 50)
    private String bookingReference;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;
    
    @Column(name = "lead_pax_id", nullable = false)
    private UUID leadPaxId;
    
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PaxEntity> paxes = new ArrayList<>();
    
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductEntity> products = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
    
    // Helper methods for bidirectional relationships
    public void addPax(PaxEntity pax) {
        paxes.add(pax);
        pax.setBooking(this);
    }
    
    public void addProduct(ProductEntity product) {
        products.add(product);
        product.setBooking(this);
    }
}
```

#### PaxEntity

```java
@Entity
@Table(name = "booking_pax")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaxEntity {
    @Id
    @Column(name = "pax_id", nullable = false)
    private UUID paxId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @ToString.Exclude
    private BookingEntity booking;
    
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    @Column(name = "phone", nullable = false, length = 50)
    private String phone;
    
    @Column(name = "age", nullable = false)
    private Integer age;
    
    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;
    
    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;
    
    @Column(name = "pax_type", nullable = false, length = 20)
    private String paxType;
}
```

#### ProductEntity

```java
@Entity
@Table(name = "booking_product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEntity {
    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @ToString.Exclude
    private BookingEntity booking;
    
    @Column(name = "search_id", nullable = false)
    private UUID searchId;
    
    @Column(name = "search_created_at", nullable = false)
    private OffsetDateTime searchCreatedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 50)
    private ProductType productType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;
    
    @Column(name = "hash", nullable = false, length = 255)
    private String hash;
    
    @Type(JsonBinaryType.class)
    @Column(name = "paxes_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> paxesIds;
    
    @Type(JsonBinaryType.class)
    @Column(name = "product_details", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> productDetails;
}
```

### JSONB Structure for Product Details

For Transfer products, the `product_details` JSONB column contains:

```json
{
  "origin": {
    "latitude": 40.7128,
    "longitude": -74.0060,
    "iata_code": "JFK",
    "full_address": "JFK Airport, NY"
  },
  "destination": {
    "latitude": 40.7589,
    "longitude": -73.9851,
    "iata_code": null,
    "full_address": "Times Square, NY"
  },
  "outbound_trip": {
    "trip_id": "uuid",
    "pickup_datetime": "2024-03-15T10:00:00Z",
    "transfer_type": "OUTBOUND",
    "vehicle": {
      "vehicle_id": "uuid",
      "type": "Sedan",
      "model": "Mercedes E-Class",
      "capacity": 4,
      "max_bags": 3,
      "max_paxes": 4
    }
  },
  "inbound_trip": null,
  "price": {
    "service_type": "PRIVATE",
    "currency": "USD",
    "total_amount": 150.00,
    "subtotal_amount": 130.00,
    "fees_and_taxes": 20.00,
    "pax_prices": []
  }
}
```

This structure:
- Preserves all transfer-specific data
- Supports nullable fields (e.g., `inbound_trip` for one-way transfers)
- Enables JSONB queries using PostgreSQL's JSON operators
- Provides flexibility for future product types (Activity, Hotel)

## Correctness Properties

A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Property Reflection

After analyzing all acceptance criteria, I identified the following testable properties. Through reflection, I've eliminated redundancy by combining related properties and removing those that are logically subsumed by others:

**Redundancy Analysis:**
- Properties 8.2, 8.3, 8.4, 8.5 (repository query methods) are all variations of the same pattern: "saved entities can be queried by specific fields". These can be combined into a single comprehensive property about query correctness.
- Properties 9.2, 9.3, 9.4 (service retrieval methods) are redundant with the repository properties since the service layer delegates to repositories.
- Properties 6.2, 6.3 (mapper converts all paxes/products) can be combined into a single property about mapping completeness.
- Properties 14.1-14.5 (JSONB structure fields) can be combined into a single property about complete serialization.
- Properties 15.3-15.6 (projection consistency checks) can be combined into a single comprehensive consistency property.
- Properties 3.4, 3.5 (cascade delete for paxes and products) can be combined with properties 5.4, 5.5 (cascade operations) into a single property about referential integrity.

### Core Properties

### Property 1: Event Subscription Idempotency

For any sequence of events, processing the same events multiple times should produce the same final projection state.

**Validates: Requirements 1.4, 18.7**

### Property 2: Complete Event Retrieval

For any aggregate ID, when retrieving events from the Event Store, all events belonging to that aggregate should be retrieved.

**Validates: Requirements 1.5**

### Property 3: Projection Handler Invocation

For any aggregate and any set of registered projection handlers, all handlers matching the aggregate type should be invoked exactly once.

**Validates: Requirements 2.4, 2.5**

### Property 4: Cascade Delete and Referential Integrity

For any booking entity with child entities (paxes and products), deleting the booking should cascade delete all children, and orphaned children should be removed when updating the parent.

**Validates: Requirements 3.4, 3.5, 5.4, 5.5, 18.5, 18.6**

### Property 5: Unique Booking Reference

For any two distinct bookings, their booking references must be unique, and attempting to save a booking with a duplicate reference should be rejected.

**Validates: Requirements 3.8**

### Property 6: Automatic Timestamp Management

For any booking entity, when created, the created_at timestamp should be set automatically, and when updated, the updated_at timestamp should be updated automatically.

**Validates: Requirements 5.9**

### Property 7: Mapping Completeness

For any booking aggregate with N passengers and M products, the mapped entity should contain exactly N passenger entities and M product entities with all fields preserved.

**Validates: Requirements 6.2, 6.3, 6.5, 6.6**

### Property 8: Product Details Serialization Round-Trip

For any transfer product, serializing to JSONB and deserializing should preserve all product-specific fields including origin, destination, trips, vehicle details, and price information.

**Validates: Requirements 6.4, 14.1, 14.2, 14.3, 14.4, 14.5, 14.7**

### Property 9: Aggregate to Entity Conversion

For any booking aggregate, converting to a PostgreSQL entity and saving should result in an entity that can be retrieved with the same booking ID and all core fields preserved.

**Validates: Requirements 7.2, 7.3**

### Property 10: Repository Query Correctness

For any saved booking entity, querying by booking reference, status, lead passenger ID, or passenger email should return the correct booking(s) with all related entities eagerly fetched when requested.

**Validates: Requirements 8.2, 8.3, 8.4, 8.5, 8.6**

### Property 11: Service Retrieval Completeness

For any booking saved to PostgreSQL, retrieving by ID or reference through the service layer should return the complete booking with all passengers and products.

**Validates: Requirements 9.2, 9.3, 9.4**

### Property 12: Snapshot Optimization Efficiency

For any aggregate with a snapshot at version V, rebuilding the aggregate should load only events with version > V, not all events from the beginning.

**Validates: Requirements 12.3, 12.7**

### Property 13: Snapshot Fallback Completeness

For any aggregate without a snapshot, rebuilding should load all events from the beginning and produce the correct final state.

**Validates: Requirements 12.4, 12.5**

### Property 14: Offset Management on Success

For any batch of events, if all projection handlers succeed, the subscription offset should be updated to reflect the last processed event.

**Validates: Requirements 13.6**

### Property 15: Offset Preservation on Failure

For any batch of events, if any projection handler fails, the subscription offset should not be updated, ensuring retry on the next poll.

**Validates: Requirements 13.2**

### Property 16: Batch Deduplication

For any batch of events containing multiple events for the same aggregate, only unique aggregate IDs should be processed, with each aggregate rebuilt once using the latest version.

**Validates: Requirements 17.1**

### Property 17: Aggregate Processing Isolation

For any two distinct aggregates in the same batch, processing one aggregate should not affect the state or processing of the other aggregate.

**Validates: Requirements 17.3, 17.4**

### Property 18: Upsert Behavior

For any booking entity, if a booking with the same ID already exists, saving should update the existing record rather than creating a duplicate.

**Validates: Requirements 18.2**

### Property 19: Projection Consistency

For any booking aggregate processed by both MongoDB and PostgreSQL projection handlers, the resulting projections should contain consistent data for booking ID, reference, status, lead passenger ID, number of passengers, and number of products.

**Validates: Requirements 15.3, 15.4, 15.5, 15.6**

### Edge Cases and Examples

The following scenarios require specific example-based tests rather than property-based tests:

**Example 1: Exception Propagation**
When a projection handler throws an exception, the exception should propagate to the event handler to trigger retry.
**Validates: Requirements 2.6, 7.6, 13.1**

**Example 2: Not Found Exception**
When retrieving a booking by ID that doesn't exist, the service should throw BookingNotFoundException.
**Validates: Requirements 9.5**

**Example 3: Optimistic Locking Conflict**
When two concurrent updates attempt to modify the same booking, the second update should fail with an optimistic locking exception.
**Validates: Requirements 5.8, 18.3, 18.4**

**Example 4: Null Handling in Optional Fields**
When mapping a one-way transfer (inbound_trip is null), the mapper should correctly handle the null value without errors.
**Validates: Requirements 6.7, 14.6**

**Example 5: Aggregate Processing Failure Isolation**
When processing a batch with multiple aggregates and one fails, the failure should not prevent other aggregates from being processed in subsequent retries.
**Validates: Requirements 17.5**

## Error Handling

### Error Handling Strategy

The system implements a fail-fast approach with automatic retry for transient failures:

#### Projection Update Failures

```java
@Override
public void handle(Aggregate aggregate) {
    try {
        // Convert and save to PostgreSQL
        BookingEntity entity = mapper.toEntity((Booking) aggregate);
        postgresService.save(entity);
        log.debug("PostgreSQL projection updated successfully");
    } catch (Exception ex) {
        log.error("Failed to update PostgreSQL projection for aggregate: {}", 
                  aggregate.getAggregateId(), ex);
        throw ex; // Propagate to trigger retry
    }
}
```

**Behavior:**
- Any exception during projection update is logged and propagated
- The event handler does not update the subscription offset
- On the next polling cycle, the same events are reprocessed
- This ensures eventual consistency even with transient failures

#### Optimistic Locking Conflicts

```java
@Entity
public class BookingEntity {
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
```

**Behavior:**
- Concurrent modifications are detected via version field
- `OptimisticLockException` is thrown on version conflict
- Exception propagates to trigger retry with fresh data
- Ensures data integrity under concurrent access

#### Not Found Scenarios

```java
public BookingEntity retrieveById(UUID bookingId) {
    return repository.findByIdWithDetails(bookingId)
        .orElseThrow(() -> new BookingNotFoundException(
            "Booking not found: " + bookingId));
}
```

**Behavior:**
- Missing bookings result in `BookingNotFoundException`
- Returns HTTP 404 to API clients
- Logged at appropriate level for monitoring

#### Database Connection Failures

**Behavior:**
- Connection pool automatically retries failed connections
- HikariCP handles connection validation and recovery
- Transient network issues are handled by connection pool
- Persistent failures are logged and require operator intervention

### Error Recovery Flow

```
1. Event Processing Attempt
   └─> Projection Handler throws exception
       └─> Exception logged with aggregate ID and details
       └─> Exception propagated to Event Handler
       └─> Subscription offset NOT updated

2. Next Polling Cycle
   └─> Same events retrieved (offset unchanged)
   └─> Retry processing
       ├─> Success: Offset updated, move forward
       └─> Failure: Repeat cycle

3. Persistent Failures
   └─> Logged at ERROR level
   └─> Metrics exposed for monitoring
   └─> Operator intervention may be required
```

### Exception Hierarchy

```
RuntimeException
├── DomainRuntimeException (Base for domain exceptions)
│   └── BookingNotFoundException (404 - Not Found)
├── OptimisticLockException (409 - Conflict)
├── DataAccessException (500 - Database Error)
│   ├── DataIntegrityViolationException (Constraint violations)
│   └── QueryTimeoutException (Query timeout)
└── ProjectionUpdateException (500 - Projection Error)
```

## Testing Strategy

### Dual Testing Approach

The testing strategy employs both unit tests and property-based tests to ensure comprehensive coverage:

**Unit Tests:**
- Specific examples demonstrating correct behavior
- Edge cases and boundary conditions
- Error handling scenarios
- Integration points between components

**Property-Based Tests:**
- Universal properties that hold for all inputs
- Comprehensive input coverage through randomization
- Minimum 100 iterations per property test
- Each test references its design document property

### Property-Based Testing Configuration

**Framework:** Use fast-check (JavaScript/TypeScript) or QuickCheck-style library for Java (e.g., jqwik)

**Configuration:**
```java
@Property
@PropertyDefaults(tries = 100) // Minimum 100 iterations
void propertyTest(@ForAll("bookingGenerator") Booking booking) {
    // Test implementation
}
```

**Tagging Convention:**
Each property test must include a comment tag referencing the design property:
```java
/**
 * Feature: booking-query-handler-postgres, Property 1: Event Subscription Idempotency
 * 
 * For any sequence of events, processing the same events multiple times 
 * should produce the same final projection state.
 */
@Property
void testEventSubscriptionIdempotency() {
    // Test implementation
}
```

### Test Categories

#### 1. Projection Handler Tests

**Unit Tests:**
- Test MongoDB projection handler with sample booking
- Test PostgreSQL projection handler with sample booking
- Test exception propagation on save failure
- Test handler filtering by aggregate type

**Property Tests:**
- Property 3: All matching handlers are invoked for any aggregate
- Property 7: Mapping preserves all fields for any booking
- Property 8: Serialization round-trip for any transfer product
- Property 9: Aggregate to entity conversion for any booking

#### 2. Repository Tests

**Unit Tests:**
- Test findByBookingReference with known reference
- Test findByStatus with specific status
- Test findByIdWithDetails includes all children
- Test unique constraint violation on duplicate reference

**Property Tests:**
- Property 5: Unique booking reference for any two distinct bookings
- Property 10: Query correctness for any saved booking
- Property 18: Upsert behavior for any booking with existing ID

#### 3. Service Layer Tests

**Unit Tests:**
- Test retrieveById throws BookingNotFoundException when not found
- Test retrieveByReference with valid reference
- Test retrieveAll returns all saved bookings
- Test caching behavior (if applicable)

**Property Tests:**
- Property 11: Service retrieval completeness for any saved booking

#### 4. Mapper Tests

**Unit Tests:**
- Test mapping booking with no products
- Test mapping booking with null inbound_trip
- Test bidirectional relationship establishment
- Test JSONB serialization of complex transfer

**Property Tests:**
- Property 7: Mapping completeness for any booking with N paxes and M products
- Property 8: Product details serialization round-trip for any transfer

#### 5. Event Handler Tests

**Unit Tests:**
- Test event handler processes single event
- Test event handler processes batch of events
- Test exception propagation on handler failure
- Test offset update on success

**Property Tests:**
- Property 1: Idempotency for any sequence of events
- Property 2: Complete event retrieval for any aggregate ID
- Property 16: Batch deduplication for any batch with duplicates
- Property 17: Aggregate processing isolation for any two aggregates

#### 6. Snapshot Optimization Tests

**Unit Tests:**
- Test snapshot retrieval when available
- Test fallback to full event replay when no snapshot
- Test event loading after snapshot version

**Property Tests:**
- Property 12: Snapshot optimization efficiency for any aggregate with snapshot
- Property 13: Snapshot fallback completeness for any aggregate without snapshot

#### 7. Cascade and Referential Integrity Tests

**Unit Tests:**
- Test cascade delete removes all children
- Test orphan removal on update
- Test foreign key constraints

**Property Tests:**
- Property 4: Cascade delete and referential integrity for any booking with children
- Property 6: Automatic timestamp management for any booking entity

#### 8. Projection Consistency Tests

**Unit Tests:**
- Test MongoDB and PostgreSQL projections match for sample booking
- Test consistency check identifies divergence
- Test consistency check passes for matching projections

**Property Tests:**
- Property 19: Projection consistency for any booking processed by both handlers

#### 9. Concurrency Tests

**Unit Tests:**
- Test optimistic locking detects concurrent modification
- Test version conflict throws exception
- Test retry after version conflict succeeds

**Example Tests:**
- Example 3: Optimistic locking conflict scenario

#### 10. Error Handling Tests

**Unit Tests:**
- Test BookingNotFoundException on missing booking
- Test exception propagation from projection handler
- Test offset preservation on failure

**Example Tests:**
- Example 1: Exception propagation
- Example 2: Not found exception
- Example 4: Null handling in optional fields
- Example 5: Aggregate processing failure isolation

### Integration Tests

Beyond unit and property tests, integration tests should verify:

- End-to-end event processing from Event Store to projections
- REST API endpoints return correct HTTP status codes
- Database migrations execute successfully
- Dual datasource configuration works correctly
- Connection pooling handles load appropriately
- Caching behavior works as expected

### Test Data Generators

For property-based tests, implement generators for:

```java
@Provide
Arbitrary<Booking> bookingGenerator() {
    return Combinators.combine(
        uuidGenerator(),
        bookingReferenceGenerator(),
        bookingStatusGenerator(),
        paxListGenerator(),
        productListGenerator()
    ).as(Booking::new);
}

@Provide
Arbitrary<Transfer> transferGenerator() {
    return Combinators.combine(
        locationGenerator(),
        locationGenerator(),
        tripGenerator(),
        tripGenerator().optional(),
        priceGenerator()
    ).as(Transfer::new);
}
```

### Performance Tests

While not part of correctness testing, performance tests should verify:

- Query response times meet SLA requirements
- Batch processing handles expected event volume
- Connection pool sizing is appropriate
- Index usage is optimal for common queries
- JSONB queries perform acceptably

### Test Coverage Goals

- Unit test coverage: >80% for all components
- Property test coverage: All 19 core properties implemented
- Integration test coverage: All critical paths
- Edge case coverage: All 5 example scenarios

## API Design

### REST Endpoints

The query-handler exposes REST endpoints for querying both MongoDB and PostgreSQL projections:

#### PostgreSQL Projection Endpoints

```
GET /query-handler/api/bookings/postgres/{bookingId}
```
- Retrieves a booking by ID from PostgreSQL projection
- Returns: `BookingEntity` with all related paxes and products
- Status: 200 OK (found), 404 Not Found (missing)

```
GET /query-handler/api/bookings/postgres
```
- Retrieves all bookings from PostgreSQL projection
- Returns: `List<BookingEntity>`
- Status: 200 OK

```
GET /query-handler/api/bookings/postgres/reference/{reference}
```
- Retrieves a booking by reference from PostgreSQL projection
- Returns: `BookingEntity`
- Status: 200 OK (found), 404 Not Found (missing)

#### MongoDB Projection Endpoints (Existing)

```
GET /query-handler/api/bookings/mongo/{bookingId}
```
- Retrieves a booking by ID from MongoDB projection
- Returns: `Mono<BookingEntity>` (reactive)
- Status: 200 OK (found), 404 Not Found (missing)

```
GET /query-handler/api/bookings/mongo
```
- Retrieves all bookings from MongoDB projection
- Returns: `Flux<BookingEntity>` (reactive)
- Status: 200 OK

### Controller Implementation

```java
@RestController
@RequestMapping("/query-handler/api")
@RequiredArgsConstructor
public class BookingQueryController {
    private final BookingProjectionService mongoService;
    private final BookingPostgresProjectionService postgresService;
    
    // PostgreSQL endpoints
    @GetMapping("/bookings/postgres/{bookingId}")
    public ResponseEntity<BookingEntity> retrieveFromPostgres(
        @PathVariable UUID bookingId) {
        return ResponseEntity.ok(postgresService.retrieveById(bookingId));
    }
    
    @GetMapping("/bookings/postgres")
    public ResponseEntity<List<BookingEntity>> retrieveAllFromPostgres() {
        return ResponseEntity.ok(postgresService.retrieveAll());
    }
    
    @GetMapping("/bookings/postgres/reference/{reference}")
    public ResponseEntity<BookingEntity> retrieveByReferenceFromPostgres(
        @PathVariable String reference) {
        return ResponseEntity.ok(postgresService.retrieveByReference(reference));
    }
    
    // MongoDB endpoints (existing)
    @GetMapping("/bookings/mongo/{bookingId}")
    public ResponseEntity<Mono<BookingEntity>> retrieveFromMongo(
        @PathVariable UUID bookingId) {
        return ResponseEntity.ok(mongoService.retrieveById(bookingId));
    }
    
    @GetMapping("/bookings/mongo")
    public ResponseEntity<Flux<BookingEntity>> retrieveAllFromMongo() {
        return ResponseEntity.ok(mongoService.retrieveAll());
    }
}
```

### Error Response Format

```json
{
  "timestamp": "2024-03-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Booking not found: 123e4567-e89b-12d3-a456-426614174000",
  "path": "/query-handler/api/bookings/postgres/123e4567-e89b-12d3-a456-426614174000"
}
```

### API Versioning

The API uses path-based versioning through the `/query-handler/api` prefix. Future versions can be introduced as `/query-handler/api/v2` if breaking changes are needed.

### Content Negotiation

All endpoints support:
- Request: No body (GET requests)
- Response: `application/json`
- Character encoding: UTF-8

### CORS Configuration

CORS should be configured to allow access from authorized frontend applications:

```yaml
spring:
  web:
    cors:
      allowed-origins: ${ALLOWED_ORIGINS:http://localhost:3000}
      allowed-methods: GET, OPTIONS
      allowed-headers: "*"
      max-age: 3600
```

## Configuration Design

### Dual Datasource Configuration

The system requires two separate datasources: one for the Event Store and one for the PostgreSQL projection.

#### Application Configuration (application.yml)

```yaml
# Event Store Configuration
eventstore:
  subscription:
    enabled: true
    name: "booking-query-projections"
    polling-interval: 1000  # Poll every second
    polling-initial-delay: 5000  # Wait 5 seconds on startup
  snapshot:
    booking-order:
      enabled: true
      frequency: 100  # Snapshot every 100 events

# Datasource Configuration
spring:
  datasource:
    # Event Store datasource (shared with command-handler)
    eventstore:
      jdbc-url: jdbc:postgresql://${EVENTSTORE_HOST:localhost}:${EVENTSTORE_PORT:5432}/${EVENTSTORE_DB:eventstore_db}
      username: ${EVENTSTORE_USER:eventstore_user}
      password: ${EVENTSTORE_PASSWORD:eventstore_pass}
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 10
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
    
    # PostgreSQL projection datasource
    postgresql:
      jdbc-url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:booking_query_db}
      username: ${POSTGRES_USER:booking_user}
      password: ${POSTGRES_PASSWORD:booking_pass}
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 20
        minimum-idle: 10
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        pool-name: PostgresProjectionPool
  
  # JPA Configuration
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
    show-sql: false
    hibernate:
      ddl-auto: validate  # Never auto-generate schema
    open-in-view: false
  
  # Flyway Configuration
  flyway:
    enabled: true
    locations: classpath:db/migration/postgresql
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    out-of-order: false
    
  # Cache Configuration
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=300s
    cache-names:
      - postgres-bookings

# Logging Configuration
logging:
  level:
    com.cjrequena.sample.query.handler: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

#### Datasource Bean Configuration

```java
@Configuration
public class DatasourceConfiguration {
    
    @Bean
    @ConfigurationProperties("spring.datasource.eventstore")
    public DataSourceProperties eventstoreDataSourceProperties() {
        return new DataSourceProperties();
    }
    
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.eventstore.hikari")
    public DataSource eventstoreDataSource() {
        return eventstoreDataSourceProperties()
            .initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.postgresql")
    public DataSourceProperties postgresqlDataSourceProperties() {
        return new DataSourceProperties();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.postgresql.hikari")
    public DataSource postgresqlDataSource() {
        return postgresqlDataSourceProperties()
            .initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }
}
```

#### JPA Configuration

```java
@Configuration
@EnableJpaRepositories(
    basePackages = "com.cjrequena.sample.query.handler.persistence.postgresql.repository",
    entityManagerFactoryRef = "postgresqlEntityManagerFactory",
    transactionManagerRef = "postgresqlTransactionManager"
)
public class PostgresqlJpaConfiguration {
    
    @Bean
    public LocalContainerEntityManagerFactoryBean postgresqlEntityManagerFactory(
        @Qualifier("postgresqlDataSource") DataSource dataSource,
        EntityManagerFactoryBuilder builder) {
        
        return builder
            .dataSource(dataSource)
            .packages("com.cjrequena.sample.query.handler.persistence.postgresql.entity")
            .persistenceUnit("postgresql")
            .properties(jpaProperties())
            .build();
    }
    
    @Bean
    public PlatformTransactionManager postgresqlTransactionManager(
        @Qualifier("postgresqlEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
    
    private Map<String, Object> jpaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.hbm2ddl.auto", "validate");
        props.put("hibernate.jdbc.batch_size", 20);
        props.put("hibernate.order_inserts", true);
        props.put("hibernate.order_updates", true);
        return props;
    }
}
```

#### Flyway Configuration

```java
@Configuration
public class FlywayConfiguration {
    
    @Bean
    public Flyway flyway(@Qualifier("postgresqlDataSource") DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/postgresql")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .validateOnMigrate(true)
            .load();
    }
    
    @Bean
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }
}
```

### Environment Variables

The following environment variables can be used to configure the system:

**Event Store:**
- `EVENTSTORE_HOST`: Event Store database host (default: localhost)
- `EVENTSTORE_PORT`: Event Store database port (default: 5432)
- `EVENTSTORE_DB`: Event Store database name (default: eventstore_db)
- `EVENTSTORE_USER`: Event Store database user (default: eventstore_user)
- `EVENTSTORE_PASSWORD`: Event Store database password (default: eventstore_pass)

**PostgreSQL Projection:**
- `POSTGRES_HOST`: PostgreSQL projection host (default: localhost)
- `POSTGRES_PORT`: PostgreSQL projection port (default: 5432)
- `POSTGRES_DB`: PostgreSQL projection database (default: booking_query_db)
- `POSTGRES_USER`: PostgreSQL projection user (default: booking_user)
- `POSTGRES_PASSWORD`: PostgreSQL projection password (default: booking_pass)

**Event Subscription:**
- `EVENTSTORE_SUBSCRIPTION_ENABLED`: Enable event subscription (default: true)
- `EVENTSTORE_SUBSCRIPTION_POLLING_INTERVAL`: Polling interval in ms (default: 1000)
- `EVENTSTORE_SUBSCRIPTION_INITIAL_DELAY`: Initial delay in ms (default: 5000)

### Database Migration Files

Migration files should be placed in `src/main/resources/db/migration/postgresql/`:

```
db/migration/postgresql/
├── V1__create_booking_schema.sql
├── V2__add_indexes.sql
└── V3__add_constraints.sql
```

Example migration file structure:

```sql
-- V1__create_booking_schema.sql
CREATE TABLE booking (
    booking_id UUID PRIMARY KEY,
    booking_reference VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,
    lead_pax_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE booking_pax (
    pax_id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    age INTEGER NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    pax_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_booking_pax_booking 
        FOREIGN KEY (booking_id) 
        REFERENCES booking(booking_id) 
        ON DELETE CASCADE
);

CREATE TABLE booking_product (
    product_id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    search_id UUID NOT NULL,
    search_created_at TIMESTAMP NOT NULL,
    product_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    hash VARCHAR(255) NOT NULL,
    paxes_ids JSONB NOT NULL,
    product_details JSONB NOT NULL,
    CONSTRAINT fk_booking_product_booking 
        FOREIGN KEY (booking_id) 
        REFERENCES booking(booking_id) 
        ON DELETE CASCADE
);
```

## Performance Design

### Snapshot Optimization Strategy

The system leverages snapshot optimization to minimize event processing and improve projection rebuilding performance:

**Without Snapshots:**
```
Aggregate with 10,000 events
└─> Load all 10,000 events from Event Store
└─> Apply all 10,000 events to reconstruct aggregate
└─> Time: ~10 seconds
```

**With Snapshots (every 100 events):**
```
Aggregate with 10,000 events
└─> Load snapshot at version 9,900
└─> Load only 100 events after snapshot
└─> Apply 100 events to snapshot
└─> Time: ~0.1 seconds (100x faster)
```

**Configuration:**
```yaml
eventstore:
  snapshot:
    booking-order:
      enabled: true
      frequency: 100  # Create snapshot every 100 events
```

**Impact:**
- New projection creation: Minutes instead of hours
- Projection rebuilding: Seconds instead of minutes
- Event Store load: Reduced by 90-99%

### Query Optimization

#### Fetch Join Strategy

To avoid N+1 query problems, use fetch joins for retrieving bookings with related entities:

```java
@Query("SELECT b FROM BookingEntity b " +
       "LEFT JOIN FETCH b.paxes " +
       "LEFT JOIN FETCH b.products " +
       "WHERE b.bookingId = :bookingId")
Optional<BookingEntity> findByIdWithDetails(@Param("bookingId") UUID bookingId);
```

**Performance Impact:**
- Without fetch join: 1 query for booking + N queries for paxes + M queries for products
- With fetch join: 1 query total
- Improvement: O(N+M) → O(1) queries

#### Index Strategy

Indexes are created for all common query patterns:

```sql
-- Unique constraint and fast lookup
CREATE INDEX idx_booking_reference ON booking(booking_reference);

-- Filter by status
CREATE INDEX idx_booking_status ON booking(status);

-- Filter by lead passenger
CREATE INDEX idx_booking_lead_pax ON booking(lead_pax_id);

-- Search by passenger email
CREATE INDEX idx_booking_pax_email ON booking_pax(email);

-- Join optimization
CREATE INDEX idx_booking_pax_booking_id ON booking_pax(booking_id);
CREATE INDEX idx_booking_product_booking_id ON booking_product(booking_id);

-- Filter by product type
CREATE INDEX idx_booking_product_type ON booking_product(product_type);

-- JSONB queries
CREATE INDEX idx_booking_product_details ON booking_product USING GIN (product_details);
```

**JSONB Query Examples:**
```sql
-- Find transfers with specific origin
SELECT * FROM booking_product 
WHERE product_details @> '{"origin": {"iata_code": "JFK"}}';

-- Find transfers with price > 100
SELECT * FROM booking_product 
WHERE (product_details->'price'->>'total_amount')::numeric > 100;
```

### Connection Pooling

HikariCP is configured with appropriate pool sizes for expected load:

**Event Store Connection Pool:**
```yaml
hikari:
  maximum-pool-size: 10  # Lower pool size (read-only access)
  minimum-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

**PostgreSQL Projection Pool:**
```yaml
hikari:
  maximum-pool-size: 20  # Higher pool size (read/write access)
  minimum-idle: 10
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
  pool-name: PostgresProjectionPool
```

**Sizing Guidelines:**
- Event Store: Lower pool size (mostly sequential polling)
- PostgreSQL: Higher pool size (concurrent API requests)
- Formula: `pool_size = (core_count * 2) + effective_spindle_count`
- Monitor: Connection wait time, pool exhaustion events

### Batch Processing

JPA batch processing is enabled to reduce database round trips:

```yaml
jpa:
  properties:
    hibernate:
      jdbc:
        batch_size: 20  # Process 20 statements per batch
      order_inserts: true  # Group inserts by entity type
      order_updates: true  # Group updates by entity type
```

**Performance Impact:**
- Without batching: 20 inserts = 20 database round trips
- With batching: 20 inserts = 1 database round trip
- Improvement: 20x reduction in network overhead

### Parallel Event Processing

Events are processed in parallel using Java parallel streams:

```java
events.parallelStream()
    .map(Event::getAggregateId)
    .distinct()
    .forEach(aggregateId -> {
        // Process each aggregate independently
        final Aggregate aggregate = retrieveOrInstantiateAggregate(aggregateId);
        updateProjections(aggregate);
    });
```

**Performance Impact:**
- Sequential: Process 100 aggregates in 100 seconds (1s each)
- Parallel (4 cores): Process 100 aggregates in 25 seconds
- Improvement: 4x throughput on multi-core systems

**Configuration:**
```java
// Use ForkJoinPool.commonPool() by default
// Or configure custom thread pool:
ForkJoinPool customPool = new ForkJoinPool(8);
customPool.submit(() -> 
    events.parallelStream().forEach(this::processEvent)
).get();
```

### Caching Strategy

Spring Cache abstraction is used to cache frequently accessed bookings:

```java
@Cacheable(value = "postgres-bookings", key = "#bookingId")
public BookingEntity retrieveById(UUID bookingId) {
    return repository.findByIdWithDetails(bookingId)
        .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));
}
```

**Cache Configuration:**
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=300s
    cache-names:
      - postgres-bookings
```

**Cache Invalidation:**
```java
@CacheEvict(value = "postgres-bookings", key = "#entity.bookingId")
public void save(BookingEntity entity) {
    repository.save(entity);
}
```

**Performance Impact:**
- Cache hit: ~1ms (in-memory lookup)
- Cache miss: ~50ms (database query with fetch joins)
- Improvement: 50x faster for cached bookings

### Read-Only Transactions

Query operations use read-only transactions to optimize database locking:

```java
@Service
@Transactional(readOnly = true)
public class BookingPostgresProjectionService {
    // All query methods benefit from read-only optimization
}
```

**Benefits:**
- Reduced lock contention
- Improved query performance
- Better database resource utilization

### Performance Monitoring

Key metrics to monitor:

**Event Processing:**
- Events processed per second
- Unique aggregates processed per batch
- Average processing time per aggregate
- Snapshot hit rate

**Database:**
- Query execution time (p50, p95, p99)
- Connection pool utilization
- Connection wait time
- Index usage statistics

**Cache:**
- Cache hit rate
- Cache eviction rate
- Cache size

**API:**
- Request latency (p50, p95, p99)
- Requests per second
- Error rate

### Performance Targets

**Event Processing:**
- Process 1000 events/second
- Rebuild aggregate with snapshot: <100ms
- Rebuild aggregate without snapshot: <1s per 1000 events

**Query Performance:**
- Retrieve booking by ID: <50ms (p95)
- Retrieve booking by reference: <50ms (p95)
- List all bookings: <200ms for 1000 bookings (p95)

**Projection Lag:**
- Maximum lag between event creation and projection update: <5 seconds (p95)

### Scalability Considerations

**Vertical Scaling:**
- Increase connection pool sizes
- Increase JVM heap size
- Increase parallel processing threads

**Horizontal Scaling:**
- Multiple query-handler instances can run concurrently
- Each instance polls independently
- Subscription offset prevents duplicate processing
- Load balancer distributes API requests

**Database Scaling:**
- PostgreSQL read replicas for query load
- Connection pooling prevents connection exhaustion
- Partitioning for very large datasets (future consideration)

## Implementation Notes

### Dependencies

Add the following dependencies to `pom.xml`:

```xml
<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Hibernate Types for JSONB support -->
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
    <version>3.7.0</version>
</dependency>

<!-- Flyway for database migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<!-- HikariCP (included with Spring Boot) -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>

<!-- Caffeine Cache -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### Package Structure

```
com.cjrequena.sample.query.handler
├── controller
│   └── BookingQueryController.java
├── service
│   ├── event
│   │   ├── EventHandler.java
│   │   ├── BookingEventHandler.java
│   │   └── ScheduledEventHandlerService.java
│   ├── projection
│   │   ├── ProjectionHandler.java (interface)
│   │   ├── BookingMongoProjectionHandler.java
│   │   └── BookingPostgresProjectionHandler.java
│   ├── BookingProjectionService.java (MongoDB)
│   └── BookingPostgresProjectionService.java
├── persistence
│   ├── mongodb
│   │   ├── entity
│   │   │   ├── BookingEntity.java
│   │   │   ├── PaxEntity.java
│   │   │   └── ProductEntity.java
│   │   └── repository
│   │       └── BookingRepository.java
│   └── postgresql
│       ├── entity
│       │   ├── BookingEntity.java
│       │   ├── PaxEntity.java
│       │   └── ProductEntity.java
│       └── repository
│           └── BookingPostgresRepository.java
├── mapper
│   └── BookingEntityMapper.java
├── configuration
│   ├── DatasourceConfiguration.java
│   ├── PostgresqlJpaConfiguration.java
│   ├── FlywayConfiguration.java
│   └── CacheConfiguration.java
└── domain
    ├── aggregate
    │   └── Booking.java
    ├── vo
    │   ├── PaxVO.java
    │   ├── ProductVO.java
    │   ├── transfer
    │   │   ├── TransferVO.java
    │   │   ├── TransferPriceVO.java
    │   │   ├── LocationVO.java
    │   │   ├── TripVO.java
    │   │   └── VehicleVO.java
    │   └── ... (other product types)
    ├── enums
    │   ├── AggregateType.java
    │   ├── BookingStatus.java
    │   ├── ProductStatus.java
    │   └── ProductType.java
    └── exception
        ├── DomainRuntimeException.java
        └── BookingNotFoundException.java
```

### Implementation Sequence

1. **Phase 1: Database Setup**
   - Create Flyway migration scripts
   - Define PostgreSQL schema
   - Test migrations locally

2. **Phase 2: Entity Layer**
   - Implement JPA entities (BookingEntity, PaxEntity, ProductEntity)
   - Configure JSONB type handling
   - Test entity persistence

3. **Phase 3: Repository Layer**
   - Implement BookingPostgresRepository
   - Add custom query methods
   - Test repository operations

4. **Phase 4: Mapper Layer**
   - Implement BookingEntityMapper
   - Handle aggregate to entity conversion
   - Test mapping completeness

5. **Phase 5: Service Layer**
   - Implement BookingPostgresProjectionService
   - Add caching
   - Test service operations

6. **Phase 6: Projection Handler**
   - Create ProjectionHandler interface
   - Implement BookingPostgresProjectionHandler
   - Refactor BookingMongoProjectionHandler
   - Test projection updates

7. **Phase 7: Event Handler**
   - Update BookingEventHandler to use projection handlers
   - Test event processing flow
   - Verify snapshot optimization

8. **Phase 8: API Layer**
   - Add PostgreSQL endpoints to controller
   - Test API responses
   - Verify error handling

9. **Phase 9: Configuration**
   - Configure dual datasources
   - Configure JPA and Flyway
   - Test configuration in different environments

10. **Phase 10: Testing**
    - Implement property-based tests
    - Implement unit tests
    - Implement integration tests
    - Verify all correctness properties

### Migration Strategy

For existing systems with data in MongoDB:

1. **Deploy with Dual Write**
   - Deploy query-handler with PostgreSQL projection enabled
   - New events automatically populate PostgreSQL
   - MongoDB remains primary for queries

2. **Backfill PostgreSQL**
   - Run backfill process to populate PostgreSQL from Event Store
   - Leverage snapshot optimization for fast backfill
   - Verify data consistency between projections

3. **Gradual Migration**
   - Gradually shift read traffic to PostgreSQL endpoints
   - Monitor performance and consistency
   - Keep MongoDB as fallback

4. **Complete Migration**
   - Once PostgreSQL is stable, make it primary
   - Optionally deprecate MongoDB endpoints
   - Or keep both for different use cases

### Monitoring and Observability

Implement the following monitoring:

```java
@Component
public class ProjectionMetrics {
    private final MeterRegistry registry;
    
    public void recordEventProcessing(int eventCount, long durationMs) {
        registry.counter("projection.events.processed", "count", String.valueOf(eventCount))
            .increment();
        registry.timer("projection.processing.duration")
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordProjectionUpdate(String projectionType, boolean success) {
        registry.counter("projection.updates", 
            "type", projectionType,
            "status", success ? "success" : "failure")
            .increment();
    }
    
    public void recordSnapshotHit(boolean hit) {
        registry.counter("projection.snapshot", 
            "result", hit ? "hit" : "miss")
            .increment();
    }
}
```

### Security Considerations

1. **Database Access**
   - Use separate database users for Event Store (read-only) and PostgreSQL (read-write)
   - Implement connection encryption (SSL/TLS)
   - Rotate credentials regularly

2. **API Security**
   - Implement authentication/authorization for API endpoints
   - Use HTTPS for all API communication
   - Implement rate limiting

3. **Data Privacy**
   - Consider encryption at rest for sensitive data
   - Implement audit logging for data access
   - Follow GDPR/privacy regulations

### Troubleshooting Guide

**Problem: Projection lag increasing**
- Check Event Store connection pool utilization
- Verify snapshot optimization is enabled
- Check for slow queries in PostgreSQL
- Monitor parallel processing thread pool

**Problem: Projection inconsistency**
- Check for failed projection updates in logs
- Verify subscription offset is advancing
- Check for version conflicts (optimistic locking)
- Run consistency verification tool

**Problem: High database load**
- Verify indexes are being used (EXPLAIN ANALYZE)
- Check connection pool sizing
- Enable query caching
- Consider read replicas

**Problem: Slow API responses**
- Check cache hit rate
- Verify fetch joins are used
- Monitor database query performance
- Check connection pool wait times

### Future Enhancements

1. **Additional Product Types**
   - Extend mapper to handle Activity and Hotel products
   - Add JSONB structure for each product type
   - Update tests for new product types

2. **Advanced Querying**
   - Implement full-text search on JSONB fields
   - Add pagination support for list endpoints
   - Implement filtering and sorting

3. **Projection Rebuilding Tool**
   - CLI tool to rebuild projections from Event Store
   - Support for selective rebuilding (by date range, aggregate ID)
   - Progress tracking and reporting

4. **Consistency Verification Tool**
   - Automated comparison between MongoDB and PostgreSQL
   - Report generation for divergences
   - Automated reconciliation

5. **Performance Optimization**
   - Implement materialized views for complex queries
   - Add database partitioning for large datasets
   - Implement read replicas for query scaling

## Diagrams

### Entity Relationship Diagram

```
┌─────────────────────────────────────────┐
│            booking                       │
├─────────────────────────────────────────┤
│ PK  booking_id          UUID            │
│     booking_reference   VARCHAR(50)     │
│     status              VARCHAR(20)     │
│     lead_pax_id         UUID            │
│     created_at          TIMESTAMP       │
│     updated_at          TIMESTAMP       │
│     version             INTEGER         │
└─────────────────────────────────────────┘
         │                    │
         │ 1                  │ 1
         │                    │
         │ N                  │ N
         ▼                    ▼
┌──────────────────────┐  ┌──────────────────────────┐
│   booking_pax        │  │   booking_product        │
├──────────────────────┤  ├──────────────────────────┤
│ PK  pax_id      UUID │  │ PK  product_id      UUID │
│ FK  booking_id  UUID │  │ FK  booking_id      UUID │
│     first_name       │  │     search_id       UUID │
│     last_name        │  │     search_created_at    │
│     email            │  │     product_type         │
│     phone            │  │     status               │
│     age              │  │     hash                 │
│     document_type    │  │     paxes_ids      JSONB │
│     document_number  │  │     product_details JSONB│
│     pax_type         │  └──────────────────────────┘
└──────────────────────┘
```

### Sequence Diagram: Event Processing

```
Event Store    ScheduledService    BookingEventHandler    ProjectionHandlers    PostgreSQL
    │                 │                    │                      │                 │
    │◄────poll────────┤                    │                      │                 │
    │                 │                    │                      │                 │
    ├─events─────────►│                    │                      │                 │
    │                 │                    │                      │                 │
    │                 ├─handle(events)────►│                      │                 │
    │                 │                    │                      │                 │
    │                 │                    ├─rebuild aggregate───►│                 │
    │◄────────────────┼────────────────────┤ (with snapshot)     │                 │
    │                 │                    │                      │                 │
    ├─snapshot + events──────────────────►│                      │                 │
    │                 │                    │                      │                 │
    │                 │                    ├─handle(aggregate)───►│                 │
    │                 │                    │                      │                 │
    │                 │                    │                      ├─map & save─────►│
    │                 │                    │                      │                 │
    │                 │                    │                      │◄────success─────┤
    │                 │                    │◄─────success─────────┤                 │
    │                 │◄───success─────────┤                      │                 │
    │                 │                    │                      │                 │
    │                 ├─update offset─────►│                      │                 │
    │◄────────────────┤                    │                      │                 │
```

## Summary

This design document specifies a comprehensive PostgreSQL projection implementation for the booking query handler that:

- Follows clean CQRS architecture with Event Store Subscription
- Leverages snapshot optimization for efficient projection rebuilding
- Uses a hybrid database schema balancing relational integrity with flexibility
- Implements dual projections (MongoDB and PostgreSQL) from a single event stream
- Provides robust error handling with automatic retry
- Optimizes performance through caching, batching, and parallel processing
- Includes comprehensive testing strategy with property-based tests
- Supports horizontal scaling and high availability

The implementation maintains backward compatibility with existing MongoDB endpoints while adding new PostgreSQL endpoints, enabling gradual migration and comparison between projection technologies.
