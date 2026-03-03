# PostgreSQL Projection Implementation Guide

## Overview
This guide outlines how to add a PostgreSQL-based projection alongside the existing MongoDB projection. It covers **three architectural approaches** based on your event sourcing setup.

---

## Current Architecture Analysis

Your system uses **Event Sourcing** with the `es-core` framework:

```
Command-Handler:
├── Receives commands
├── Updates aggregates
├── Persists events to Event Store (PostgreSQL)
├── ScheduledEventHandlerService polls Event Store
└── BookingEventHandler
    ├── Reads new events
    ├── Publishes to Kafka (optional)
    └── (Currently) Updates MongoDB projection ❌

Query-Handler:
└── Reads from MongoDB projection
```

---

## Three Architectural Approaches

### Approach 1: Event Store Subscription (Recommended) ⭐⭐⭐

**Best for:** Modular monolith, single deployment, shared event store

```
┌─────────────────────────────────────────────────────────────┐
│                   Command-Handler                            │
│                   (Pure Command Side)                        │
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
│                   Query-Handler                              │
│                   (Pure Query Side)                          │
├─────────────────────────────────────────────────────────────┤
│  1. ScheduledEventHandlerService polls Event Store          │
│  2. BookingEventHandler processes new events                │
│  3. Rebuilds aggregate from events                          │
│  4. Updates MongoDB projection                              │
│  5. Updates PostgreSQL projection                           │
│  6. Exposes query endpoints                                 │
└─────────────────────────────────────────────────────────────┘
```

**Pros:**
- ✅ True CQRS separation
- ✅ Command-handler stays pure (no projection logic)
- ✅ No Kafka infrastructure needed
- ✅ Direct event store access
- ✅ Easy to add more projections
- ✅ Can rebuild projections from event store
- ✅ Simpler architecture
- ✅ Lower operational complexity

**Cons:**
- ⚠️ Query-handler needs event store access
- ⚠️ Polling overhead (minimal with proper config)

**When to use:**
- Single application deployment
- Shared event store database
- Want simplest architecture
- Don't need external event consumers

---

### Approach 2: Kafka Event Bus

**Best for:** Microservices, multiple event consumers, event streaming

```
┌─────────────────────────────────────────────────────────────┐
│                   Command-Handler                            │
├─────────────────────────────────────────────────────────────┤
│  1. Receive command                                         │
│  2. Update aggregate                                        │
│  3. Persist events to Event Store                           │
│  4. Publish events to Kafka ✅                              │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │     Kafka     │
                    │  (Event Bus)  │
                    └───────────────┘
                            │
                ┌───────────┴───────────┬───────────────┐
                ▼                       ▼               ▼
┌───────────────────────┐  ┌──────────────────┐  ┌─────────┐
│   Query-Handler       │  │  Analytics       │  │  Audit  │
├───────────────────────┤  │  Service         │  │ Service │
│  1. Subscribe to      │  └──────────────────┘  └─────────┘
│     Kafka events      │
│  2. Rebuild aggregate │
│  3. Update MongoDB    │
│  4. Update PostgreSQL │
└───────────────────────┘
```

**Pros:**
- ✅ Microservices ready
- ✅ Multiple event consumers
- ✅ Event replay capabilities
- ✅ Decoupled services
- ✅ Scalable independently

**Cons:**
- ⚠️ Requires Kafka infrastructure
- ⚠️ More complex operations
- ⚠️ Network overhead
- ⚠️ Need to handle event ordering
- ⚠️ Need idempotency
- ❌ **Cannot leverage snapshot optimization** - New consumers must process ALL events from Kafka history
- ❌ **No aggregate rebuilding from snapshots** - Loses the performance benefit of your snapshot strategy
- ❌ **Kafka retention limits** - May not have full event history for rebuilding projections

**When to use:**
- Microservices architecture
- Multiple services need same events
- Real-time event streaming
- Services in different networks
- **NOT recommended if you need snapshot optimization for projection rebuilding**

---

### Approach 3: Dual Write (Not Recommended)

**Current implementation - should be refactored**

```
┌─────────────────────────────────────────────────────────────┐
│                   Command-Handler                            │
├─────────────────────────────────────────────────────────────┤
│  1. Receive command                                         │
│  2. Update aggregate                                        │
│  3. Persist events to Event Store                           │
│  4. Update MongoDB projection ❌                            │
│  5. Update PostgreSQL projection ❌                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   Query-Handler                              │
├─────────────────────────────────────────────────────────────┤
│  1. Read from MongoDB                                       │
│  2. Read from PostgreSQL                                    │
└─────────────────────────────────────────────────────────────┘
```

**Pros:**
- ✅ Simple to understand
- ✅ Immediate consistency

**Cons:**
- ❌ Violates CQRS separation
- ❌ Command-handler knows about query side
- ❌ Tight coupling
- ❌ Command-handler becomes slower
- ❌ Hard to scale independently
- ❌ Can't add projections without modifying command-handler

**When to use:**
- Never (legacy pattern only)

---

## Recommended Implementation: Approach 1 (Event Store Subscription)

This is the **best approach** for your current architecture.

---

## Complexity Assessment

### Overall Complexity: **MEDIUM** ⚠️

| Module | Aspect | Complexity | Effort |
|--------|--------|-----------|--------|
| **Command-Handler** | Database Schema Design | Medium | 30 mins |
| | JPA Entity Implementation | Low-Medium | 30 mins |
| | Mapper (Aggregate → Entity) | Medium | 30 mins |
| | Dual Write Handler | Low | 15 mins |
| | Configuration | Low | 15 mins |
| **Query-Handler** | Entity Duplication/Sharing | Low | 15 mins |
| | Repository with Queries | Low | 30 mins |
| | Service Layer | Low | 30 mins |
| | Controller Updates | Low | 15 mins |
| **Both Modules** | Testing | Medium | 2-3 hours |
| | Documentation | Low | 30 mins |
| **Total Estimated Effort** | | | **3.5-4 hours (code)** |
| | | | **+ 2-3 hours (testing)** |

### Why Medium Complexity?

**Easier Parts:**
- ✅ Domain model already exists (VOs and Aggregates)
- ✅ Event structure is defined
- ✅ CQRS pattern already established
- ✅ Spring Data JPA is well-documented
- ✅ MongoDB projection exists as reference

**Challenging Parts:**
- ⚠️ Relational schema design for hierarchical data
- ⚠️ Handling polymorphic products (Transfer, Activity, Hotel)
- ⚠️ JSON columns vs normalized tables decision
- ⚠️ Dual projection synchronization (MongoDB + PostgreSQL)
- ⚠️ Entity sharing between command and query modules
- ⚠️ Migration strategy for existing data

---

## Architecture Options

### Option 1: Fully Normalized Schema (Traditional Relational)

**Structure:**
```
booking
├── booking_pax (1:N)
├── booking_product (1:N)
    ├── product_transfer (1:1)
    │   ├── transfer_location (2:1 - origin/destination)
    │   ├── transfer_trip (1:2 - departure/return)
    │   │   └── trip_vehicle (1:1)
    │   └── transfer_price (1:1)
    │       └── transfer_pax_price (1:N)
    └── product_activity (1:1) [future]
```

**Pros:**
- ✅ Full relational integrity
- ✅ Efficient joins and queries
- ✅ Easy to add indexes
- ✅ Standard SQL queries

**Cons:**
- ❌ Many tables (10-15 tables)
- ❌ Complex joins for full booking
- ❌ More code to maintain
- ❌ Schema migrations more complex

**Complexity:** HIGH

---

### Option 2: Hybrid Approach (Recommended) ⭐

**Structure:**
```
booking (main table)
├── booking_pax (1:N) - normalized
├── booking_product (1:N) - normalized
    └── product_details (JSONB column) - denormalized
```

**Pros:**
- ✅ Balance between relational and document
- ✅ Fewer tables (3-5 tables)
- ✅ PostgreSQL JSONB is powerful
- ✅ Easy to query common fields
- ✅ Flexible for nested data

**Cons:**
- ⚠️ JSONB queries less efficient than joins
- ⚠️ Some data duplication
- ⚠️ Mixed query patterns

**Complexity:** MEDIUM (Recommended)

---

### Option 3: Pure JSONB (Document-like)

**Structure:**
```
booking (single table)
├── booking_id (UUID)
├── booking_reference (VARCHAR)
├── status (VARCHAR)
├── booking_data (JSONB) - entire booking as JSON
```

**Pros:**
- ✅ Simplest schema (1 table)
- ✅ Similar to MongoDB
- ✅ Easy to implement
- ✅ Fast writes

**Cons:**
- ❌ Loses relational benefits
- ❌ Complex JSONB queries
- ❌ Harder to maintain referential integrity
- ❌ Why use PostgreSQL then?

**Complexity:** LOW (but defeats purpose of PostgreSQL)

---

## Detailed Implementation Guide

This section provides step-by-step implementation for each architectural approach.

---

## Snapshot Optimization Comparison

This is a **critical architectural difference** between approaches:

### Approach 1: Event Store Subscription ✅

```java
// New projection subscription starts
BookingEventHandler processes new events → 
  For each aggregate:
    1. Check if snapshot exists (e.g., at version 9,500)
    2. Load snapshot (instant)
    3. Replay only events after snapshot (500 events)
    4. Update projection
    
Result: Fast projection rebuilding, even for old aggregates
```

**Performance Example:**
- Aggregate with 10,000 events
- Snapshot at version 9,500
- **Only 500 events processed** ⚡
- Projection ready in seconds

### Approach 2: Kafka Event Bus ❌

```java
// New projection subscription starts
Kafka Consumer processes events from beginning →
  For each aggregate:
    1. No snapshot available in Kafka
    2. Must replay ALL events from Kafka history
    3. If Kafka retention < full history, CANNOT rebuild
    4. Update projection
    
Result: Slow projection rebuilding, potential data loss
```

**Performance Example:**
- Aggregate with 10,000 events
- No snapshot in Kafka
- **All 10,000 events must be processed** 🐌
- Projection ready in minutes/hours
- **If Kafka retention is 7 days, events older than 7 days are LOST**

### Approach 2 Hybrid: Kafka + Event Store Access ⚠️

```java
// New projection subscription starts
Kafka Consumer receives event →
  For each event:
    1. Use aggregateId from Kafka event
    2. Query event store directly for snapshot
    3. Replay events from snapshot
    4. Update projection
    
Result: Fast rebuilding, but defeats Kafka's decoupling purpose
```

**Performance Example:**
- Aggregate with 10,000 events
- Snapshot at version 9,500 (from event store)
- **Only 500 events processed** ⚡
- But query-handler needs event store access ⚠️

### Why This Matters

**Scenario: Adding a New Projection**

You want to add Elasticsearch for full-text search:

| Approach | What Happens | Time to Build |
|----------|--------------|---------------|
| **Event Store Subscription** | New handler polls event store, uses snapshots, processes recent events only | Minutes |
| **Kafka (Simple)** | New consumer replays all Kafka history, no snapshots | Hours/Days |
| **Kafka (Hybrid)** | Consumer queries event store for snapshots | Minutes (but needs event store access) |

**Scenario: Projection Corruption**

Your PostgreSQL projection gets corrupted and needs rebuilding:

| Approach | What Happens | Can Rebuild? |
|----------|--------------|--------------|
| **Event Store Subscription** | Reset subscription offset, replay from snapshots | ✅ Yes, always |
| **Kafka (Simple)** | Replay from Kafka history | ⚠️ Only if Kafka has full history |
| **Kafka (Hybrid)** | Query event store directly | ✅ Yes, always |

---

## Recommended Approach Decision Tree

```
Do you need multiple independent services consuming events?
│
├─ NO → Use Approach 1 (Event Store Subscription) ⭐
│       - Simplest architecture
│       - Full snapshot optimization
│       - Easy to add new projections
│
└─ YES → Can query-handler access the event store database?
         │
         ├─ YES → Use Hybrid Strategy 1 (Kafka as Notification) ⭐⭐
         │        - Kafka for pub/sub and decoupling
         │        - Event store for snapshot optimization
         │        - Best of both worlds
         │        - Lightweight Kafka messages
         │
         └─ NO → Must query-handler be fully decoupled from event store?
                 │
                 ├─ YES → Use Approach 2 (Pure Kafka) ⚠️
                 │        - Accept slower projection rebuilding
                 │        - Ensure Kafka retention covers full history
                 │        - Or implement Strategy 2 (Snapshots in Kafka)
                 │
                 └─ NO → Reconsider architecture
                         - Why can't query-handler access event store?
                         - Consider Hybrid Strategy 1 instead
```

### Quick Recommendation Guide

| Your Situation | Recommended Approach | Reason |
|----------------|---------------------|---------|
| Single application, shared database | **Approach 1** | Simplest, full optimization |
| Microservices, shared event store | **Hybrid Strategy 1** | Kafka benefits + snapshots |
| Microservices, no shared database | **Approach 2 + Strategy 2** | Complex but fully decoupled |
| Need fast projection rebuilding | **Approach 1 or Hybrid 1** | Snapshot optimization |
| Aggregates have few events (<100) | **Any approach** | Performance difference minimal |
| Aggregates have many events (>1000) | **Avoid pure Kafka** | Snapshot optimization critical |
| Multiple event consumers | **Hybrid Strategy 1** | Kafka pub/sub + optimization |
| Adding projections frequently | **Approach 1 or Hybrid 1** | Fast rebuilding essential |

---

## Implementation: Approach 1 (Event Store Subscription) - RECOMMENDED ⭐

This is the cleanest CQRS implementation. The command-handler stays pure, and the query-handler owns all projection logic.

### Step 1: Command-Handler Cleanup (Remove Projection Logic)

#### 1.1 Remove Projection Files

Delete these files from command-handler:
```
❌ service/projection/BookingProjectionHandler.java
❌ service/projection/BookingProjectionService.java
❌ persistence/mongodb/entity/* (move to query-handler)
❌ persistence/mongodb/repository/* (move to query-handler)
```

#### 1.2 Move Event Handler Files to Query-Handler

Move these files from command-handler to query-handler:
```
service/event/EventHandler.java → query-handler
service/event/BookingEventHandler.java → query-handler
service/event/ScheduledEventHandlerService.java → query-handler
```

**Command-handler is now pure!** ✅ It only handles commands and persists events.

---

### Step 2: Query-Handler Setup (Event Subscription + Projections)

#### 2.1 Add Dependencies to pom.xml

```xml
<!-- es-core for Event Store access -->
<dependency>
    <groupId>com.cjrequena.sample</groupId>
    <artifactId>es-core</artifactId>
    <version>${project.version}</version>
</dependency>

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
```

#### 2.2 Configure Event Store + PostgreSQL in application.yml

```yaml
# Event Store Configuration (for polling events)
eventstore:
  subscription:
    enabled: true
    name: "booking-query-projections"
    polling-interval: 1000  # Poll every second
    polling-initial-delay: 5000  # Wait 5 seconds on startup

# Event Store Database (shared with command-handler)
spring:
  datasource:
    eventstore:
      url: jdbc:postgresql://localhost:5432/eventstore_db
      username: ${EVENTSTORE_USER:eventstore_user}
      password: ${EVENTSTORE_PASSWORD:eventstore_pass}
      driver-class-name: org.postgresql.Driver
    
    # PostgreSQL for projections
    postgresql:
      url: jdbc:postgresql://localhost:5432/booking_query_db
      username: ${POSTGRES_USER:booking_user}
      password: ${POSTGRES_PASSWORD:booking_pass}
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 20
        minimum-idle: 10
  
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false
    hibernate:
      ddl-auto: validate
  
  flyway:
    enabled: true
    locations: classpath:db/migration/postgresql
    baseline-on-migrate: true
```

#### 2.3 Create Flyway Migration

**File:** `src/main/resources/db/migration/postgresql/V1__create_booking_schema.sql`

(See full schema in Database Schema Design section below)

#### 2.4 Move and Modify BookingEventHandler

**File:** `query-handler/.../service/event/BookingEventHandler.java`

```java
package com.cjrequena.sample.query.handler.service.event;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import com.cjrequena.sample.es.core.service.EventStoreService;
import com.cjrequena.sample.query.handler.service.projection.ProjectionHandler;
import jakarta.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Event handler for booking events in the query-handler.
 * <p>
 * This handler:
 * 1. Polls the event store for new events
 * 2. Rebuilds aggregates from events
 * 3. Updates all projections (MongoDB + PostgreSQL)
 * </p>
 */
@Component
@Transactional
@Log4j2
public class BookingEventHandler extends EventHandler {

  private final List<ProjectionHandler> projectionHandlers;

  @Autowired
  public BookingEventHandler(
    EventStoreService eventStoreService,
    EventMapper eventMapper,
    EventStoreConfigurationProperties eventStoreConfigurationProperties,
    List<ProjectionHandler> projectionHandlers
  ) {
    super(eventStoreService, eventMapper, eventStoreConfigurationProperties);
    this.projectionHandlers = projectionHandlers;
  }

  @Override
  public void handle(List<EventEntity> eventEntityList) {
    log.info("Processing {} events for booking projections", eventEntityList.size());

    final List<Event> events = this.eventMapper.toEventList(eventEntityList);

    // Process each unique aggregate
    events.parallelStream()
      .map(Event::getAggregateId)
      .distinct()
      .forEach(aggregateId -> {
        log.debug("Rebuilding aggregate and updating projections for: {}", aggregateId);
        
        // Rebuild aggregate from event store
        final Aggregate aggregate = retrieveOrInstantiateAggregate(aggregateId);
        
        // Update all registered projections
        projectionHandlers.stream()
          .filter(handler -> handler.getAggregateType().getType().equals(aggregate.getAggregateType()))
          .forEach(handler -> {
            try {
              handler.handle(aggregate);
              log.debug("Updated {} projection for: {}", handler.getClass().getSimpleName(), aggregateId);
            } catch (Exception ex) {
              log.error("Failed to update projection: {}", handler.getClass().getSimpleName(), ex);
              throw ex; // Fail fast - will retry on next poll
            }
          });
      });
  }

  @Nonnull
  @Override
  public AggregateType getAggregateType() {
    return AggregateType.BOOKING_ORDER;
  }
}
```

#### 2.5 Create Projection Handler Interface

**File:** `query-handler/.../service/projection/ProjectionHandler.java`

```java
package com.cjrequena.sample.query.handler.service.projection;

import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;

/**
 * Interface for projection handlers.
 * <p>
 * Each projection (MongoDB, PostgreSQL, Elasticsearch, etc.)
 * implements this interface to update its projection from aggregates.
 * </p>
 */
public interface ProjectionHandler {
    
    /**
     * Updates the projection with the given aggregate.
     *
     * @param aggregate the aggregate to project
     */
    void handle(Aggregate aggregate);
    
    /**
     * Returns the aggregate type this handler processes.
     *
     * @return the aggregate type
     */
    AggregateType getAggregateType();
}
```

#### 2.6 Create MongoDB Projection Handler

**File:** `query-handler/.../service/projection/BookingMongoProjectionHandler.java`

```java
package com.cjrequena.sample.query.handler.service.projection;

import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projection handler for MongoDB.
 * <p>
 * Updates the MongoDB projection when booking events are processed.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingMongoProjectionHandler implements ProjectionHandler {

    private final BookingMongoProjectionService mongoService;

    @Override
    @Transactional
    public void handle(Aggregate aggregate) {
        log.debug("Updating MongoDB projection for: {}", aggregate.getAggregateId());
        
        Booking booking = (Booking) aggregate;
        mongoService.save(booking);
        
        log.debug("MongoDB projection updated successfully");
    }

    @Override
    public AggregateType getAggregateType() {
        return AggregateType.BOOKING_ORDER;
    }
}
```

#### 2.7 Create PostgreSQL Projection Handler

**File:** `query-handler/.../service/projection/BookingPostgresProjectionHandler.java`

```java
package com.cjrequena.sample.query.handler.service.projection;

import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projection handler for PostgreSQL.
 * <p>
 * Updates the PostgreSQL projection when booking events are processed.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingPostgresProjectionHandler implements ProjectionHandler {

    private final BookingPostgresProjectionService postgresService;

    @Override
    @Transactional
    public void handle(Aggregate aggregate) {
        log.debug("Updating PostgreSQL projection for: {}", aggregate.getAggregateId());
        
        Booking booking = (Booking) aggregate;
        postgresService.save(booking);
        
        log.debug("PostgreSQL projection updated successfully");
    }

    @Override
    public AggregateType getAggregateType() {
        return AggregateType.BOOKING_ORDER;
    }
}
```

---

### Step 3: PostgreSQL Entities, Repositories, and Services

(See Database Schema Design and JPA Entity Implementation sections below for complete details)

---

## Implementation: Approach 2 (Kafka Event Bus)

If you need Kafka for microservices or multiple event consumers:

### ⚠️ Critical Limitation: No Snapshot Support

**Important**: When using Kafka, new query-handler subscriptions cannot leverage the snapshot optimization that exists in your event store. This means:

1. **Full Event Replay Required**: A new projection must process ALL events from Kafka history
2. **No Snapshot Shortcut**: Cannot use `retrieveAggregateFromSnapshot()` to skip old events
3. **Performance Impact**: For aggregates with thousands of events, this is significantly slower
4. **Kafka Retention Limits**: If Kafka doesn't retain full history, you cannot rebuild projections

**Example Scenario:**
```
Event Store Subscription (Approach 1):
- Aggregate has 10,000 events
- Latest snapshot at event 9,500
- New projection only processes 500 events ✅

Kafka Event Bus (Approach 2):
- Aggregate has 10,000 events
- No snapshot available in Kafka
- New projection must process all 10,000 events ❌
- If Kafka retention is 7 days, older events are lost ❌
```

### When Kafka Makes Sense Despite This Limitation

Use Kafka only if:
- You have **multiple independent services** that need the same events
- You need **real-time event streaming** to external systems
- Your aggregates are **small** (few events per aggregate)
- You **don't plan to add new projections** frequently
- You accept the **performance trade-off**

### ⚠️ Critical Optimization: Batch Processing and Deduplication

**Problem Identified:**
If you receive 100 Kafka notifications for the same aggregate, the naive implementation would:
1. Rebuild aggregate from snapshot 100 times
2. Update projections 100 times
3. Waste 99 rebuilds - only the last state matters!

**Example:**
```
Kafka messages for aggregate ABC:
1. BookingPlaced (version 1)
2. PaxAdded (version 2)
3. PaxUpdated (version 3)
...
100. BookingConfirmed (version 100)

Naive approach: Rebuild 100 times ❌
Optimized approach: Rebuild once at version 100 ✅
```

### Solution 1: Batch Processing with Deduplication (Recommended)

Process Kafka messages in batches, keeping only the latest notification per aggregate.

```java
@Component
@Log4j2
@RequiredArgsConstructor
public class BookingEventKafkaConsumer {

    private final List<ProjectionHandler> projectionHandlers;
    private final EventStoreService eventStoreService;
    private final EventMapper eventMapper;
    private final EventStoreConfigurationProperties eventStoreConfigurationProperties;

    /**
     * Batch consumer that processes multiple notifications efficiently.
     * Only rebuilds each aggregate once, using the latest version.
     */
    @Bean
    public Consumer<List<EventNotification>> bookingEventBatchConsumer() {
        return notifications -> {
            log.info("Received batch of {} notifications from Kafka", notifications.size());
            
            // Deduplicate: Keep only the latest notification per aggregate
            Map<UUID, EventNotification> latestByAggregate = notifications.stream()
                .collect(Collectors.toMap(
                    EventNotification::getAggregateId,
                    Function.identity(),
                    (existing, replacement) -> 
                        existing.getAggregateVersion() > replacement.getAggregateVersion() 
                            ? existing 
                            : replacement
                ));
            
            log.info("Deduplicated to {} unique aggregates", latestByAggregate.size());
            
            // Process each unique aggregate once
            latestByAggregate.values().parallelStream()
                .forEach(notification -> {
                    try {
                        processNotification(notification);
                    } catch (Exception ex) {
                        log.error("Failed to process aggregate {}", 
                            notification.getAggregateId(), ex);
                        throw ex; // Fail fast - Kafka will retry the batch
                    }
                });
        };
    }
    
    private void processNotification(EventNotification notification) {
        log.debug("Processing aggregate {} at version {}", 
            notification.getAggregateId(), notification.getAggregateVersion());
        
        // Rebuild aggregate from event store with snapshot optimization
        Aggregate aggregate = retrieveOrInstantiateAggregate(notification.getAggregateId());
        
        // Update all projections once
        projectionHandlers.stream()
            .filter(handler -> handler.getAggregateType().getType()
                .equals(aggregate.getAggregateType()))
            .forEach(handler -> handler.handle(aggregate));
    }

    // ... rest of the snapshot optimization methods ...
}
```

**Configuration for Batch Processing:**

```yaml
spring:
  cloud:
    stream:
      bindings:
        bookingEventBatchConsumer-in-0:
          destination: booking-notifications
          group: booking-query-handler
          consumer:
            batch-mode: true  # Enable batch processing
            max-attempts: 3
      kafka:
        bindings:
          bookingEventBatchConsumer-in-0:
            consumer:
              batch-size: 100  # Process up to 100 messages at once
              batch-timeout: 1000  # Or wait max 1 second
        consumer:
          enable-auto-commit: false
          max-poll-records: 100  # Fetch up to 100 records per poll
```

**Performance Impact:**

| Scenario | Naive Approach | Batch + Deduplication |
|----------|---------------|----------------------|
| 100 events for same aggregate | 100 rebuilds | 1 rebuild ✅ |
| 100 events for 10 aggregates | 100 rebuilds | 10 rebuilds ✅ |
| 100 events for 100 aggregates | 100 rebuilds | 100 rebuilds (same) |

---

### Solution 2: Time-Window Aggregation (Advanced)

Collect notifications in a time window, then process deduplicated batch.

```java
@Component
@Log4j2
@RequiredArgsConstructor
public class BookingEventWindowedConsumer {

    private final List<ProjectionHandler> projectionHandlers;
    private final EventStoreService eventStoreService;
    
    // In-memory buffer for time-window aggregation
    private final Map<UUID, EventNotification> notificationBuffer = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @PostConstruct
    public void init() {
        // Process buffer every 5 seconds
        scheduler.scheduleAtFixedRate(
            this::processBuffer, 
            5, 5, TimeUnit.SECONDS
        );
    }
    
    /**
     * Kafka consumer that buffers notifications in a time window.
     */
    @Bean
    public Consumer<EventNotification> bookingEventConsumer() {
        return notification -> {
            // Add to buffer, keeping only latest version per aggregate
            notificationBuffer.compute(
                notification.getAggregateId(),
                (key, existing) -> {
                    if (existing == null || 
                        existing.getAggregateVersion() < notification.getAggregateVersion()) {
                        return notification;
                    }
                    return existing;
                }
            );
            
            log.debug("Buffered notification for aggregate {} (buffer size: {})", 
                notification.getAggregateId(), notificationBuffer.size());
        };
    }
    
    private void processBuffer() {
        if (notificationBuffer.isEmpty()) {
            return;
        }
        
        log.info("Processing buffer with {} unique aggregates", notificationBuffer.size());
        
        // Drain buffer
        Map<UUID, EventNotification> toProcess = new HashMap<>(notificationBuffer);
        notificationBuffer.clear();
        
        // Process in parallel
        toProcess.values().parallelStream()
            .forEach(notification -> {
                try {
                    Aggregate aggregate = retrieveOrInstantiateAggregate(
                        notification.getAggregateId()
                    );
                    updateProjections(aggregate);
                } catch (Exception ex) {
                    log.error("Failed to process aggregate {}", 
                        notification.getAggregateId(), ex);
                    // Re-add to buffer for retry
                    notificationBuffer.put(notification.getAggregateId(), notification);
                }
            });
    }
    
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        processBuffer(); // Process remaining notifications
    }
}
```

**Pros:**
- ✅ Maximum deduplication (5-second window)
- ✅ Reduces projection updates significantly
- ✅ Better throughput for high-frequency updates

**Cons:**
- ⚠️ Eventual consistency delay (up to 5 seconds)
- ⚠️ In-memory buffer (lost on crash)
- ⚠️ More complex error handling

---

### Solution 3: Kafka Streams with Windowing (Production-Grade)

Use Kafka Streams to deduplicate at the Kafka level before consuming.

```java
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean
    public KStream<String, EventNotification> bookingNotificationStream(
        StreamsBuilder builder
    ) {
        // Source: Raw notifications
        KStream<String, EventNotification> notifications = builder
            .stream("booking-notifications-raw");
        
        // Deduplicate using windowed aggregation
        KTable<Windowed<String>, EventNotification> deduplicated = notifications
            .groupByKey()
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(5)))
            .reduce(
                (existing, replacement) -> 
                    existing.getAggregateVersion() > replacement.getAggregateVersion()
                        ? existing
                        : replacement,
                Materialized.as("deduplicated-notifications")
            );
        
        // Output: Deduplicated notifications
        deduplicated
            .toStream()
            .map((windowedKey, notification) -> 
                KeyValue.pair(windowedKey.key(), notification))
            .to("booking-notifications-deduplicated");
        
        return notifications;
    }
}

@Component
@Log4j2
@RequiredArgsConstructor
public class BookingEventConsumer {

    /**
     * Consume from deduplicated topic.
     * Each aggregate appears only once per 5-second window.
     */
    @Bean
    public Consumer<EventNotification> bookingEventConsumer() {
        return notification -> {
            log.info("Processing deduplicated notification for aggregate {}", 
                notification.getAggregateId());
            
            Aggregate aggregate = retrieveOrInstantiateAggregate(
                notification.getAggregateId()
            );
            updateProjections(aggregate);
        };
    }
}
```

**Configuration:**

```yaml
spring:
  cloud:
    stream:
      kafka:
        streams:
          binder:
            configuration:
              application.id: booking-query-handler
              commit.interval.ms: 1000
      bindings:
        bookingEventConsumer-in-0:
          destination: booking-notifications-deduplicated  # Consume deduplicated
          group: booking-query-handler
```

**Pros:**
- ✅ Deduplication at Kafka level (before consumer)
- ✅ Scalable and fault-tolerant
- ✅ No in-memory buffer in consumer
- ✅ Production-grade solution

**Cons:**
- ⚠️ Requires Kafka Streams infrastructure
- ⚠️ More complex setup
- ⚠️ Additional Kafka topics

---

### Solution 4: Smart Projection Handler with Versioning

Add version checking in projection handlers to skip unnecessary updates.

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingPostgresProjectionHandler implements ProjectionHandler {

    private final BookingPostgresProjectionService postgresService;
    private final BookingPostgresRepository repository;

    @Override
    @Transactional
    public void handle(Aggregate aggregate) {
        Booking booking = (Booking) aggregate;
        
        // Check if we already have this version or newer
        Optional<BookingEntity> existing = repository.findById(booking.getBookingId());
        
        if (existing.isPresent()) {
            Integer currentVersion = existing.get().getVersion();
            Long newVersion = booking.getAggregateVersion();
            
            if (currentVersion >= newVersion) {
                log.debug("Skipping projection update - already at version {} (new: {})",
                    currentVersion, newVersion);
                return; // Skip - we already have this version
            }
        }
        
        log.debug("Updating PostgreSQL projection for: {} (version {})", 
            aggregate.getAggregateId(), booking.getAggregateVersion());
        
        postgresService.save(booking);
    }

    @Override
    public AggregateType getAggregateType() {
        return AggregateType.BOOKING_ORDER;
    }
}
```

**Pros:**
- ✅ Simple to implement
- ✅ Works with any consumer approach
- ✅ Idempotent projection updates
- ✅ Protects against out-of-order messages

**Cons:**
- ⚠️ Still rebuilds aggregate (just skips projection update)
- ⚠️ Database query per notification
- ⚠️ Doesn't prevent redundant rebuilding

---

### Recommended Combination: Solution 1 + Solution 4

Use **batch processing with deduplication** (Solution 1) to minimize rebuilds, plus **version checking** (Solution 4) for safety.

```java
@Component
@Log4j2
@RequiredArgsConstructor
public class OptimizedBookingEventConsumer {

    private final List<ProjectionHandler> projectionHandlers;
    private final EventStoreService eventStoreService;

    /**
     * Batch consumer with deduplication.
     * Processes each aggregate only once per batch.
     */
    @Bean
    public Consumer<List<EventNotification>> bookingEventBatchConsumer() {
        return notifications -> {
            log.info("Received batch of {} notifications", notifications.size());
            
            // Step 1: Deduplicate - keep only latest per aggregate
            Map<UUID, EventNotification> latestByAggregate = notifications.stream()
                .collect(Collectors.toMap(
                    EventNotification::getAggregateId,
                    Function.identity(),
                    (existing, replacement) -> 
                        existing.getAggregateVersion() > replacement.getAggregateVersion() 
                            ? existing 
                            : replacement
                ));
            
            log.info("Deduplicated to {} unique aggregates (saved {} rebuilds)", 
                latestByAggregate.size(), 
                notifications.size() - latestByAggregate.size());
            
            // Step 2: Process each unique aggregate once
            latestByAggregate.values().parallelStream()
                .forEach(notification -> {
                    try {
                        // Rebuild aggregate from snapshot
                        Aggregate aggregate = retrieveOrInstantiateAggregate(
                            notification.getAggregateId()
                        );
                        
                        // Update projections (with version checking inside handlers)
                        projectionHandlers.stream()
                            .filter(handler -> handler.getAggregateType().getType()
                                .equals(aggregate.getAggregateType()))
                            .forEach(handler -> handler.handle(aggregate));
                            
                    } catch (Exception ex) {
                        log.error("Failed to process aggregate {}", 
                            notification.getAggregateId(), ex);
                        throw ex;
                    }
                });
        };
    }
    
    // ... snapshot optimization methods ...
}
```

**Performance Comparison:**

| Scenario | Naive | Batch Dedup | Batch + Version Check |
|----------|-------|-------------|----------------------|
| 100 events, 1 aggregate | 100 rebuilds, 100 updates | 1 rebuild, 1 update ✅ | 1 rebuild, 1 update ✅ |
| 100 events, 10 aggregates (10 each) | 100 rebuilds, 100 updates | 10 rebuilds, 10 updates ✅ | 10 rebuilds, 10 updates ✅ |
| Out-of-order messages | Incorrect state ❌ | Incorrect state ❌ | Correct state ✅ |
| Duplicate processing | Duplicate updates ❌ | Duplicate updates ❌ | Skipped ✅ |

---

If you must use Kafka but want snapshot support, there are several strategies:

#### Strategy 1: Kafka for Notifications, Event Store for Data (Recommended Hybrid)

Use Kafka events as **notifications only**, then fetch full aggregate from event store with snapshot support.

```java
@Component
@Log4j2
@RequiredArgsConstructor
public class BookingEventKafkaConsumer {

    private final List<ProjectionHandler> projectionHandlers;
    private final EventStoreService eventStoreService;
    private final EventMapper eventMapper;
    private final EventStoreConfigurationProperties eventStoreConfigurationProperties;

    /**
     * Kafka consumer that uses events as notifications only.
     * Actual aggregate rebuilding uses event store with snapshot optimization.
     */
    @Bean
    public Consumer<Event> bookingEventConsumer() {
        return event -> {
            log.info("Received event notification from Kafka: {} for aggregate {}", 
                event.getEventType(), event.getAggregateId());
            
            // Use Kafka event as a notification/trigger only
            // Rebuild aggregate from event store WITH snapshot support
            Aggregate aggregate = retrieveOrInstantiateAggregate(event.getAggregateId());
            
            // Update all projections with the fully rebuilt aggregate
            projectionHandlers.stream()
                .filter(handler -> handler.getAggregateType().getType().equals(aggregate.getAggregateType()))
                .forEach(handler -> {
                    try {
                        handler.handle(aggregate);
                        log.debug("Updated {} projection for: {}", 
                            handler.getClass().getSimpleName(), event.getAggregateId());
                    } catch (Exception ex) {
                        log.error("Failed to update projection: {}", 
                            handler.getClass().getSimpleName(), ex);
                        throw ex;
                    }
                });
        };
    }

    /**
     * Retrieves aggregate from event store with snapshot optimization.
     * This is the SAME logic used in Approach 1 (Event Store Subscription).
     */
    protected Aggregate retrieveOrInstantiateAggregate(UUID aggregateId) {
        final EventStoreConfigurationProperties.SnapshotProperties snapshotConfiguration = 
            eventStoreConfigurationProperties.getSnapshot(AggregateType.BOOKING_ORDER.getType());
        
        if (snapshotConfiguration.enabled()) {
            return retrieveAggregateFromSnapshot(aggregateId)
                .orElseGet(() -> createAndReproduceAggregate(aggregateId));
        } else {
            return createAndReproduceAggregate(aggregateId);
        }
    }

    protected Optional<Aggregate> retrieveAggregateFromSnapshot(UUID aggregateId) {
        final Optional<? extends Aggregate> optionalAggregate = 
            eventStoreService.retrieveAggregateSnapshot(
                AggregateType.BOOKING_ORDER.getClazz(), 
                aggregateId, 
                null
            );
        
        return optionalAggregate.map(aggregate -> {
            // Load only events AFTER the snapshot
            List<Event> events = retrieveEvents(aggregateId, aggregate.getAggregateVersion());
            aggregate.reproduceFromEvents(events);
            return aggregate;
        });
    }

    protected Aggregate createAndReproduceAggregate(UUID aggregateId) {
        log.info("Snapshot not found for Aggregate ID: {}. Reconstituting from events.", aggregateId);
        Aggregate aggregate = AggregateFactory.newInstance(
            AggregateType.BOOKING_ORDER.getClazz(), 
            aggregateId
        );
        List<Event> events = retrieveEvents(aggregateId, null);
        aggregate.reproduceFromEvents(events);
        return aggregate;
    }

    protected List<Event> retrieveEvents(UUID aggregateId, Long fromVersion) {
        return eventMapper.toEventList(
            eventStoreService.retrieveEventsByAggregateId(aggregateId, fromVersion, null)
        );
    }
}
```

**Configuration:**

```yaml
spring:
  cloud:
    stream:
      bindings:
        bookingEventConsumer-in-0:
          destination: booking-events
          group: booking-query-handler
      kafka:
        binder:
          brokers: localhost:9092
        consumer:
          enable-auto-commit: false  # Manual commit after successful projection update
          
# Event Store access for snapshot optimization
eventstore:
  snapshot:
    booking-order:
      enabled: true
      frequency: 100  # Snapshot every 100 events
```

**Pros:**
- ✅ Gets Kafka's decoupling and pub/sub benefits
- ✅ **Full snapshot optimization** - only processes events after snapshot
- ✅ Can rebuild projections efficiently
- ✅ Multiple services can consume Kafka events
- ✅ Kafka events are lightweight (just notifications)

**Cons:**
- ⚠️ Query-handler needs event store access (shared database)
- ⚠️ Two data sources to manage (Kafka + Event Store)
- ⚠️ Slightly more complex than pure Approach 1
- ⚠️ Network calls to event store for each Kafka message

**When to use:**
- You need Kafka for other consumers (analytics, audit, external systems)
- You want snapshot optimization for fast projection rebuilding
- Query-handler can access event store database
- You accept the additional complexity

---

#### Strategy 2: Kafka with Snapshot Events (Advanced)

Publish snapshots to Kafka alongside regular events.

```java
// In command-handler: Publish snapshots to Kafka
@Component
@Log4j2
public class SnapshotPublisher {

    private final StreamBridge streamBridge;
    
    public void publishSnapshot(Aggregate aggregate) {
        SnapshotEvent snapshot = SnapshotEvent.builder()
            .aggregateId(aggregate.getAggregateId())
            .aggregateType(aggregate.getAggregateType())
            .aggregateVersion(aggregate.getAggregateVersion())
            .snapshotData(serializeAggregate(aggregate))
            .timestamp(OffsetDateTime.now())
            .build();
            
        streamBridge.send("booking-snapshots", snapshot);
        log.info("Published snapshot for aggregate {} at version {}", 
            aggregate.getAggregateId(), aggregate.getAggregateVersion());
    }
}

// In query-handler: Consume both events and snapshots
@Component
@Log4j2
public class BookingKafkaConsumer {

    private final Map<UUID, CachedSnapshot> snapshotCache = new ConcurrentHashMap<>();
    
    @Bean
    public Consumer<Event> bookingEventConsumer() {
        return event -> {
            UUID aggregateId = event.getAggregateId();
            
            // Check if we have a cached snapshot
            CachedSnapshot snapshot = snapshotCache.get(aggregateId);
            
            if (snapshot != null && snapshot.getVersion() < event.getAggregateVersion()) {
                // Rebuild from snapshot + events after snapshot
                Aggregate aggregate = deserializeSnapshot(snapshot);
                aggregate.applyEvent(event);
                updateProjections(aggregate);
            } else {
                // No snapshot, need to rebuild from all events
                // This is where you'd need event store access or full Kafka history
                rebuildFromEventStore(aggregateId);
            }
        };
    }
    
    @Bean
    public Consumer<SnapshotEvent> bookingSnapshotConsumer() {
        return snapshotEvent -> {
            // Cache the snapshot
            snapshotCache.put(
                snapshotEvent.getAggregateId(),
                new CachedSnapshot(
                    snapshotEvent.getAggregateVersion(),
                    snapshotEvent.getSnapshotData()
                )
            );
            log.info("Cached snapshot for aggregate {} at version {}", 
                snapshotEvent.getAggregateId(), 
                snapshotEvent.getAggregateVersion());
        };
    }
}
```

**Pros:**
- ✅ Snapshots available in Kafka
- ✅ No event store access needed
- ✅ True decoupling from event store

**Cons:**
- ❌ Snapshots are large (increases Kafka storage)
- ❌ Complex snapshot management in Kafka
- ❌ Need to handle snapshot compaction
- ❌ New consumers still need to process from earliest snapshot
- ❌ Significant implementation complexity

**When to use:**
- Query-handler CANNOT access event store
- You have Kafka infrastructure with large storage
- You're willing to implement complex snapshot management

---

#### Strategy 3: Hybrid Subscription (Best of Both Worlds)

Use BOTH Kafka and Event Store Subscription, with Kafka as primary and event store as fallback.

```java
@Component
@Log4j2
public class HybridBookingEventHandler {

    private final List<ProjectionHandler> projectionHandlers;
    private final EventStoreService eventStoreService;
    private final Set<UUID> processedAggregates = ConcurrentHashMap.newKeySet();

    /**
     * Primary: Kafka consumer for real-time updates
     */
    @Bean
    public Consumer<Event> kafkaEventConsumer() {
        return event -> {
            log.debug("Processing event from Kafka: {}", event.getAggregateId());
            
            // Mark as processed via Kafka
            processedAggregates.add(event.getAggregateId());
            
            // Rebuild with snapshot optimization
            Aggregate aggregate = retrieveOrInstantiateAggregate(event.getAggregateId());
            updateProjections(aggregate);
        };
    }

    /**
     * Fallback: Event Store Subscription for missed events or new projections
     * Runs periodically to catch anything Kafka missed
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    public void eventStoreSubscriptionFallback() {
        log.debug("Running event store subscription fallback");
        
        // Process events from event store that weren't processed via Kafka
        List<EventEntity> events = eventStoreService.retrieveEventsByAggregateTypeAfterOffsetTxIdAndOffsetId(
            AggregateType.BOOKING_ORDER.getType(),
            lastProcessedTxId,
            lastProcessedId
        );
        
        events.stream()
            .map(EventEntity::getAggregateId)
            .distinct()
            .filter(aggregateId -> !processedAggregates.contains(aggregateId))
            .forEach(aggregateId -> {
                log.info("Processing missed aggregate from event store: {}", aggregateId);
                Aggregate aggregate = retrieveOrInstantiateAggregate(aggregateId);
                updateProjections(aggregate);
            });
    }
}
```

**Pros:**
- ✅ Real-time updates via Kafka
- ✅ Guaranteed consistency via event store fallback
- ✅ Full snapshot optimization
- ✅ Resilient to Kafka failures

**Cons:**
- ⚠️ Most complex implementation
- ⚠️ Need to track processed aggregates
- ⚠️ Potential duplicate processing (need idempotency)

**When to use:**
- Mission-critical projections
- Need both real-time and guaranteed consistency
- Can tolerate additional complexity

---

### Comparison of Hybrid Strategies

| Strategy | Snapshot Support | Event Store Access | Complexity | Best For |
|----------|-----------------|-------------------|------------|----------|
| **1. Kafka as Notification** | ✅ Full | Required | Low | Most use cases |
| **2. Snapshots in Kafka** | ⚠️ Partial | Not required | High | True decoupling needed |
| **3. Hybrid Subscription** | ✅ Full | Required | Very High | Mission-critical systems |

### Recommended Hybrid: Strategy 1 (Kafka as Notification)

For your use case, **Strategy 1** is the best hybrid approach:

```
Command-Handler:
  1. Persist events to Event Store
  2. Publish lightweight notification to Kafka
     (just aggregateId, eventType, version)

Query-Handler:
  1. Consume Kafka notification
  2. Use aggregateId to query Event Store
  3. Load snapshot (if available)
  4. Replay events after snapshot
  5. Update all projections
```

**Implementation Example:**

```java
// Command-Handler: Publish lightweight notification
Message<EventNotification> message = MessageBuilder
    .withPayload(EventNotification.builder()
        .aggregateId(event.getAggregateId())
        .aggregateType(event.getAggregateType())
        .eventType(event.getEventType())
        .aggregateVersion(event.getAggregateVersion())
        .timestamp(event.getTimestamp())
        .build())
    .setHeader(KafkaHeaders.KEY, event.getAggregateId().toString())
    .build();
    
streamBridge.send(KAFKA_BOOKING_NOTIFICATIONS, message);
```

This gives you:
- ✅ Kafka's pub/sub benefits (multiple consumers, decoupling)
- ✅ Full snapshot optimization (fast projection rebuilding)
- ✅ Small Kafka messages (just notifications, not full events)
- ✅ Event store remains source of truth
- ⚠️ Query-handler needs event store access (acceptable trade-off)

---

### Step 1: Keep Kafka Publishing in Command-Handler

The existing `BookingEventHandler` already publishes to Kafka - keep it as is.

### Step 2: Query-Handler Subscribes to Kafka (Simple Approach - No Snapshots)

#### 2.1 Add Kafka Dependencies

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-binder-kafka</artifactId>
</dependency>
```

#### 2.2 Create Kafka Consumer

```java
@Component
@Log4j2
public class BookingEventKafkaConsumer {

    private final List<ProjectionHandler> projectionHandlers;
    private final EventStoreService eventStoreService;

    @Bean
    public Consumer<Event> bookingEventConsumer() {
        return event -> {
            log.info("Received event from Kafka: {}", event.getEventType());
            
            // Rebuild aggregate from event store
            Aggregate aggregate = rebuildAggregate(event.getAggregateId());
            
            // Update all projections
            projectionHandlers.forEach(handler -> handler.handle(aggregate));
        };
    }
}
```

#### 2.3 Configure Kafka

```yaml
spring:
  cloud:
    stream:
      bindings:
        bookingEventConsumer-in-0:
          destination: booking-events
          group: booking-query-handler
      kafka:
        binder:
          brokers: localhost:9092
```

---

## Implementation: Approach 3 (Dual Write - Not Recommended)

Only use this if you must keep projection logic in command-handler.

### Modify BookingProjectionHandler

```java
@Component
@RequiredArgsConstructor
public class BookingProjectionHandler implements ProjectionHandler {

    private final BookingMongoProjectionService mongoService;
    private final BookingPostgresProjectionService postgresService;

    @Override
    public void handle(Aggregate aggregate) {
        Booking booking = (Booking) aggregate;

        // Write to both projections
        mongoService.save(booking);
        postgresService.save(booking);
    }
}
```

This violates CQRS separation and is not recommended.

---

## Database Schema Design

### Core Tables

```sql
-- Main booking table
CREATE TABLE booking (
    booking_id UUID PRIMARY KEY,
    booking_reference VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,
    lead_pax_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);

-- Passenger table
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
    CONSTRAINT fk_booking_pax_booking FOREIGN KEY (booking_id) 
        REFERENCES booking(booking_id) ON DELETE CASCADE
);

-- Product table with JSONB for type-specific data
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
    CONSTRAINT fk_booking_product_booking FOREIGN KEY (booking_id) 
        REFERENCES booking(booking_id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_booking_reference ON booking(booking_reference);
CREATE INDEX idx_booking_status ON booking(status);
CREATE INDEX idx_booking_lead_pax ON booking(lead_pax_id);
CREATE INDEX idx_booking_pax_email ON booking_pax(email);
CREATE INDEX idx_booking_pax_booking_id ON booking_pax(booking_id);
CREATE INDEX idx_booking_product_booking_id ON booking_product(booking_id);
CREATE INDEX idx_booking_product_type ON booking_product(product_type);
CREATE INDEX idx_booking_product_details ON booking_product USING GIN (product_details);
```

---

### 2. JPA Entity Implementationage com.cjrequena.sample.command.handler.service.projection;

import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingProjectionHandler implements ProjectionHandler {

    private final BookingProjectionService mongoProjectionService;
    private final BookingPostgresProjectionService postgresProjectionService;

    @Override
    public void handle(Aggregate aggregate) {
        log.debug("Saving booking to dual projections: {}", aggregate);

        Booking booking = (Booking) aggregate;

        // Write to MongoDB (existing)
        try {
            mongoProjectionService.save(booking);
            log.debug("Saved to MongoDB: {}", booking.getBookingId());
        } catch (Exception ex) {
            log.error("Failed to save to MongoDB", ex);
            // Decide: fail fast or continue?
            throw ex; // Fail fast approach
        }

        // Write to PostgreSQL (new)
        try {
            postgresProjectionService.save(booking);
            log.debug("Saved to PostgreSQL: {}", booking.getBookingId());
        } catch (Exception ex) {
            log.error("Failed to save to PostgreSQL", ex);
            // Decide: fail fast or continue?
            throw ex; // Fail fast approach
        }
    }

    @Override
    public AggregateType getAggregateType() {
        return AggregateType.BOOKING_ORDER;
    }
}
```

#### BookingEntity (Query-Handler)
-- Main booking table
CREATE TABLE booking (
    booking_id UUID PRIMARY KEY,
    booking_reference VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,
    lead_pax_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);

-- Passenger table (normalized)
CREATE TABLE booking_pax (
    pax_id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES booking(booking_id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    age INTEGER NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    pax_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_booking_pax_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id)
);

-- Product table (polymorphic)
CREATE TABLE booking_product (
    product_id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES booking(booking_id) ON DELETE CASCADE,
    search_id UUID NOT NULL,
    search_created_at TIMESTAMP NOT NULL,
    product_type VARCHAR(50) NOT NULL, -- 'Transfer', 'Activity', 'Hotel'
    status VARCHAR(20) NOT NULL,
    hash VARCHAR(255) NOT NULL,
    paxes_ids JSONB NOT NULL, -- Array of UUIDs
    product_details JSONB NOT NULL, -- Type-specific data (Transfer, Activity, etc.)
    CONSTRAINT fk_booking_product_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id)
);

-- Indexes for performance
CREATE INDEX idx_booking_reference ON booking(booking_reference);
CREATE INDEX idx_booking_status ON booking(status);
CREATE INDEX idx_booking_lead_pax ON booking(lead_pax_id);
CREATE INDEX idx_booking_pax_email ON booking_pax(email);
CREATE INDEX idx_booking_pax_booking_id ON booking_pax(booking_id);
CREATE INDEX idx_booking_product_booking_id ON booking_product(booking_id);
CREATE INDEX idx_booking_product_type ON booking_product(product_type);
CREATE INDEX idx_booking_product_details ON booking_product USING GIN (product_details);
```

#### JSONB Structure for product_details

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
  "outbound_trip": null,
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

#### PaxEntity (Query-Handler)

```java
package com.cjrequena.sample.query.handler.persistence.postgresql.entity;

import com.cjrequena.sample.query.handler.domain.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "booking", indexes = {
    @Index(name = "idx_booking_reference", columnList = "booking_reference", unique = true),
    @Index(name = "idx_booking_status", columnList = "status"),
    @Index(name = "idx_booking_lead_pax", columnList = "lead_pax_id")
})
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

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PaxEntity> paxes = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
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

#### ProductEntity (Query-Handler)

```java
package com.cjrequena.sample.query.handler.persistence.postgresql.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "booking_pax", indexes = {
    @Index(name = "idx_booking_pax_email", columnList = "email"),
    @Index(name = "idx_booking_pax_booking_id", columnList = "booking_id")
})
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

---

### 3. Repository Layer (Query-Handler)

```java
package com.cjrequena.sample.query.handler.persistence.postgresql.entity;

import com.cjrequena.sample.query.handler.domain.enums.ProductStatus;
import com.cjrequena.sample.query.handler.domain.enums.ProductType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "booking_product", indexes = {
    @Index(name = "idx_booking_product_booking_id", columnList = "booking_id"),
    @Index(name = "idx_booking_product_type", columnList = "product_type")
})
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

---

### 4. Service Layer (Query-Handler)

```java
package com.cjrequena.sample.query.handler.persistence.postgresql.repository;

import com.cjrequena.sample.query.handler.domain.enums.BookingStatus;
import com.cjrequena.sample.query.handler.persistence.postgresql.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

---

### 5. Mapper Layer (Query-Handler)

```java
package com.cjrequena.sample.query.handler.service;

import com.cjrequena.sample.query.handler.domain.exception.BookingNotFoundException;
import com.cjrequena.sample.query.handler.persistence.postgresql.entity.BookingEntity;
import com.cjrequena.sample.query.handler.persistence.postgresql.repository.BookingPostgresRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@Log4j2
@RequiredArgsConstructor
public class BookingPostgresProjectionService {

    private final BookingPostgresRepository repository;

    @Cacheable(value = "postgres-bookings", key = "#bookingId")
    public BookingEntity retrieveById(UUID bookingId) {
        log.debug("Retrieving booking from PostgreSQL: {}", bookingId);
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
}
```

---

### 6. Controller Updates (Query-Handler)

**Option A: Separate Endpoints (Recommended)**

```java
package com.cjrequena.sample.query.handler.controller;

import com.cjrequena.sample.query.handler.persistence.mongodb.entity.BookingEntity as MongoBookingEntity;
import com.cjrequena.sample.query.handler.persistence.postgresql.entity.BookingEntity as PostgresBookingEntity;
import com.cjrequena.sample.query.handler.service.BookingProjectionService;
import com.cjrequena.sample.query.handler.service.BookingPostgresProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/query-handler/api")
@RequiredArgsConstructor
public class BookingQueryController {

    private final BookingProjectionService mongoService;
    private final BookingPostgresProjectionService postgresService;

    // MongoDB endpoints (existing)
    @GetMapping("/bookings/mongo/{bookingId}")
    public ResponseEntity<Mono<MongoBookingEntity>> retrieveFromMongo(
        @PathVariable UUID bookingId) {
        return ResponseEntity.ok(mongoService.retrieveById(bookingId));
    }

    // PostgreSQL endpoints (new)
    @GetMapping("/bookings/postgres/{bookingId}")
    public ResponseEntity<PostgresBookingEntity> retrieveFromPostgres(
        @PathVariable UUID bookingId) {
        return ResponseEntity.ok(postgresService.retrieveById(bookingId));
    }

    @GetMapping("/bookings/postgres")
    public ResponseEntity<List<PostgresBookingEntity>> retrieveAllFromPostgres() {
        return ResponseEntity.ok(postgresService.retrieveAll());
    }

    @GetMapping("/bookings/postgres/reference/{reference}")
    public ResponseEntity<PostgresBookingEntity> retrieveByReferenceFromPostgres(
        @PathVariable String reference) {
        return ResponseEntity.ok(postgresService.retrieveByReference(reference));
    }
}
```

**Option B: Query Parameter**

```java
@GetMapping("/bookings/{bookingId}")
public ResponseEntity<?> retrieve(
    @PathVariable UUID bookingId,
    @RequestParam(defaultValue = "mongo") String source) {
    
    if ("postgres".equalsIgnoreCase(source)) {
        return ResponseEntity.ok(postgresService.retrieveById(bookingId));
    }
    return ResponseEntity.ok(mongoService.retrieveById(bookingId));
}
```

---

## Time Estimates

### Approach 1: Event Store Subscription (Recommended)

| Task | Estimated Time |
|------|----------------|
| Move event handlers to query-handler | 1-2 hours |
| Create ProjectionHandler interface | 30 mins |
| Implement MongoDB projection handler | 1 hour |
| Create PostgreSQL schema (Flyway) | 1-2 hours |
| Implement PostgreSQL entities | 2-3 hours |
| Implement PostgreSQL projection handler | 2-3 hours |
| Create mappers (Aggregate → Entity) | 2-3 hours |
| Update configuration | 1 hour |
| Testing and debugging | 3-4 hours |
| **Total** | **14-19 hours** |

### Approach 2: Kafka Event Bus

| Task | Estimated Time |
|------|----------------|
| All tasks from Approach 1 | 14-19 hours |
| Configure Kafka consumer | 2-3 hours |
| Handle idempotency | 2-3 hours |
| Testing Kafka integration | 2-3 hours |
| **Total** | **20-28 hours** |

**⚠️ Important Notes:**
- Does NOT include time for initial projection building (could be hours/days depending on event volume)
- New projections cannot leverage snapshot optimization
- Requires Kafka to retain full event history for projection rebuilding
- Consider Hybrid approach if you need snapshot support (adds 2-3 hours for event store integration)

### Approach 3: Dual Write (Not Recommended)

| Task | Estimated Time |
|------|----------------|
| PostgreSQL schema and entities | 3-5 hours |
| Mappers and services | 3-4 hours |
| Update projection handler | 1 hour |
| Testing | 2-3 hours |
| **Total** | **9-13 hours** |

---

## Summary

The guide now covers three main architectural approaches plus hybrid strategies:

### Main Approaches

1. **Approach 1 (Event Store Subscription)** ⭐ - Recommended for single applications. Clean CQRS separation, no Kafka needed, query-handler polls event store and updates all projections. **Fully leverages snapshot optimization** for fast projection rebuilding.

2. **Approach 2 (Kafka Event Bus)** ⚠️ - For microservices architecture with multiple event consumers. **Critical limitation: Cannot leverage snapshot optimization** - new projections must process all events from Kafka history, which can take hours/days for large aggregates. Kafka retention limits may prevent full projection rebuilding.

3. **Approach 3 (Dual Write)** ❌ - Not recommended, violates CQRS principles.

### Hybrid Strategies (Best of Both Worlds)

**Hybrid Strategy 1: Kafka as Notification** ⭐⭐ - **RECOMMENDED for microservices**
- Kafka publishes lightweight notifications (aggregateId, eventType, version)
- Query-handler uses notification to trigger event store query with snapshot optimization
- Gets Kafka's pub/sub benefits + full snapshot optimization
- Small Kafka messages, fast projection rebuilding
- **This is the answer to your question!**

**Hybrid Strategy 2: Snapshots in Kafka** (Advanced)
- Publish both events and snapshots to Kafka
- Complex snapshot management
- True decoupling but high complexity

**Hybrid Strategy 3: Dual Subscription** (Mission-Critical)
- Kafka for real-time + Event Store for guaranteed consistency
- Most complex but most resilient

### Key Architectural Insight: Snapshot Optimization

Your event sourcing implementation includes a powerful snapshot optimization:

```java
protected Aggregate retrieveOrInstantiateAggregate(UUID aggregateId) {
    if (snapshotConfiguration.enabled()) {
        return retrieveAggregateFromSnapshot(aggregateId)  // Load from snapshot
            .orElseGet(() -> createAndReproduceAggregate(aggregateId));
    }
}
```

**Snapshot support by approach:**

- ✅ **Approach 1**: Full snapshot support - new projections rebuild in minutes
- ❌ **Approach 2 (Pure Kafka)**: No snapshot support - new projections rebuild in hours/days
- ✅ **Hybrid Strategy 1**: Full snapshot support + Kafka benefits - **BEST CHOICE for microservices**
- ⚠️ **Hybrid Strategy 2**: Partial snapshot support - complex implementation
- ✅ **Hybrid Strategy 3**: Full snapshot support - highest complexity

**Real-World Impact:**

Imagine you have 100,000 bookings, each with an average of 50 events (5 million total events):

| Scenario | Approach 1 | Pure Kafka | Hybrid Strategy 1 |
|----------|-----------|------------|-------------------|
| Add new projection | ~30 minutes | ~8-10 hours | ~30 minutes |
| Process per aggregate | 500 events (from snapshot) | 5,000 events (all) | 500 events (from snapshot) |
| Rebuild corrupted projection | Always possible | Only if Kafka retains history | Always possible |
| Multiple event consumers | No | Yes | Yes |
| Kafka message size | N/A | Large (full events) | Small (notifications) |

### Recommendation

**For Single Application / Modular Monolith:**
- Use **Approach 1 (Event Store Subscription)** - simplest and most efficient

**For Microservices / Multiple Event Consumers:**
- Use **Hybrid Strategy 1 (Kafka as Notification)** - gets Kafka's benefits while keeping snapshot optimization
- This is the sweet spot: Kafka for decoupling + Event Store for performance

**Avoid Pure Kafka (Approach 2) unless:**
- Query-handler absolutely cannot access event store
- Aggregates are small (<100 events each)
- You rarely add new projections
- You're willing to accept slow projection rebuilding

The implementation is straightforward with the hybrid PostgreSQL schema (normalized tables + JSONB for nested data), providing the best balance between relational benefits and implementation simplicity.




