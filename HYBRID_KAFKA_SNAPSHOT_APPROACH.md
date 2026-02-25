# Hybrid Kafka + Snapshot Optimization Approach

## The Problem You Identified

When using pure Kafka for event distribution, new query-handler subscriptions cannot leverage snapshot optimization. This means:

- ❌ Must process ALL events from Kafka history
- ❌ No snapshot shortcut available
- ❌ Slow projection rebuilding (hours instead of minutes)
- ❌ Kafka retention limits may prevent full rebuilding

## The Solution: Kafka as Notification + Event Store for Data

Use Kafka events as **lightweight notifications**, then fetch full aggregate state from event store with snapshot optimization.

### Architecture Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   Command-Handler                            │
├─────────────────────────────────────────────────────────────┤
│  1. Receive command                                         │
│  2. Update aggregate                                        │
│  3. Persist events to Event Store ✅                        │
│  4. Publish NOTIFICATION to Kafka (lightweight)             │
│     - aggregateId: UUID                                     │
│     - eventType: String                                     │
│     - version: Long                                         │
│     - timestamp: OffsetDateTime                             │
│     (NOT the full event data!)                              │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │     Kafka     │
                    │ (Notifications)│
                    └───────────────┘
                            │
                ┌───────────┴───────────┬───────────────┐
                ▼                       ▼               ▼
┌───────────────────────┐  ┌──────────────────┐  ┌─────────┐
│   Query-Handler       │  │  Analytics       │  │  Audit  │
├───────────────────────┤  │  Service         │  │ Service │
│  1. Receive Kafka     │  └──────────────────┘  └─────────┘
│     notification      │
│  2. Extract           │
│     aggregateId       │
│  3. Query Event Store │◄─────────────────────────┐
│     for aggregate     │                          │
│  4. Load SNAPSHOT ✅  │                          │
│  5. Replay events     │                    ┌─────┴─────┐
│     after snapshot    │                    │Event Store│
│  6. Update MongoDB    │                    │(PostgreSQL│
│  7. Update PostgreSQL │                    │+ Snapshots│
└───────────────────────┘                    └───────────┘
```

### Key Benefits

✅ **Kafka Pub/Sub Benefits**
- Multiple consumers can subscribe
- Decoupling between command and query sides
- Event streaming to external systems
- Kafka's reliability and scalability

✅ **Full Snapshot Optimization**
- Load snapshot from event store
- Only replay events after snapshot
- Fast projection rebuilding (minutes, not hours)
- Can always rebuild from event store

✅ **Small Kafka Messages**
- Notifications are tiny (~100 bytes)
- Reduces Kafka storage requirements
- Faster Kafka processing
- Lower network bandwidth

✅ **Event Store as Source of Truth**
- Full event history always available
- Snapshots managed by event store
- No Kafka retention concerns
- Can rebuild projections anytime

## Critical Optimization: Avoiding Redundant Rebuilds

### The Problem You Identified

If you receive 100 Kafka notifications for the same aggregate, the naive implementation would:
1. Rebuild aggregate from snapshot 100 times
2. Update projections 100 times  
3. Waste 99 rebuilds - only the final state matters!

**Example:**
```
Kafka notifications for Booking ABC:
Message 1: BookingPlaced (version 1)
Message 2: PaxAdded (version 2)
Message 3: PaxUpdated (version 3)
...
Message 100: BookingConfirmed (version 100)

❌ Naive: Rebuild 100 times, project 100 times
✅ Optimized: Rebuild once at version 100, project once
```

### Solution: Batch Processing with Deduplication

Process Kafka messages in batches, keeping only the latest notification per aggregate.

```java
@Component
@Log4j2
@RequiredArgsConstructor
public class OptimizedBookingEventConsumer {

    private final List<ProjectionHandler> projectionHandlers;
    private final EventStoreService eventStoreService;
    private final EventMapper eventMapper;
    private final EventStoreConfigurationProperties eventStoreConfigurationProperties;

    /**
     * Batch consumer that processes multiple notifications efficiently.
     * Deduplicates to process each aggregate only once per batch.
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
            
            int savedRebuilds = notifications.size() - latestByAggregate.size();
            log.info("Deduplicated to {} unique aggregates (saved {} rebuilds)", 
                latestByAggregate.size(), savedRebuilds);
            
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
                });[HYBRID_KAFKA_SNAPSHOT_APPROACH.md](HYBRID_KAFKA_SNAPSHOT_APPROACH.md)
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
            List<Event> events = retrieveEvents(aggregateId, aggregate.getAggregateVersion());
            aggregate.reproduceFromEvents(events);
            
            log.debug("Rebuilt aggregate {} from snapshot at version {} + {} new events",
                aggregateId, aggregate.getAggregateVersion(), events.size());
            
            return aggregate;
        });
    }

    protected Aggregate createAndReproduceAggregate(UUID aggregateId) {
        log.info("Snapshot not found for Aggregate ID: {}. Reconstituting from all events.", 
            aggregateId);
        
        Aggregate aggregate = AggregateFactory.newInstance(
            AggregateType.BOOKING_ORDER.getClazz(), 
            aggregateId
        );
        
        List<Event> events = retrieveEvents(aggregateId, null);
        aggregate.reproduceFromEvents(events);
        
        log.debug("Rebuilt aggregate {} from {} events", aggregateId, events.size());
        
        return aggregate;
    }

    protected List<Event> retrieveEvents(UUID aggregateId, Long fromVersion) {
        return eventMapper.toEventList(
            eventStoreService.retrieveEventsByAggregateId(aggregateId, fromVersion, null)
        );
    }
}
```

### Configuration for Batch Processing

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
            back-off-initial-interval: 1000
      kafka:
        bindings:
          bookingEventBatchConsumer-in-0:
            consumer:
              batch-size: 100  # Process up to 100 messages at once
              batch-timeout: 1000  # Or wait max 1 second
        consumer:
          enable-auto-commit: false
          max-poll-records: 100  # Fetch up to 100 records per poll
          auto-offset-reset: earliest
```

### Additional Safety: Version Checking in Projection Handlers

Add version checking to handle out-of-order messages and duplicate processing.

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

### Performance Impact

**Scenario: 100 Kafka messages for same aggregate**

| Approach | Rebuilds | Projection Updates | Time |
|----------|----------|-------------------|------|
| Naive (no optimization) | 100 | 100 | ~5 seconds |
| Batch deduplication | 1 ✅ | 1 ✅ | ~50ms |
| Savings | 99% fewer | 99% fewer | 100x faster |

**Scenario: 100 messages for 10 aggregates (10 each)**

| Approach | Rebuilds | Projection Updates | Time |
|----------|----------|-------------------|------|
| Naive | 100 | 100 | ~5 seconds |
| Batch deduplication | 10 ✅ | 10 ✅ | ~500ms |
| Savings | 90% fewer | 90% fewer | 10x faster |

**Scenario: 100 messages for 100 aggregates (1 each)**

| Approach | Rebuilds | Projection Updates | Time |
|----------|----------|-------------------|------|
| Naive | 100 | 100 | ~5 seconds |
| Batch deduplication | 100 | 100 | ~5 seconds |
| Savings | None (but no harm) | None | Same |

### Why This Matters

In real-world scenarios:
- User updates booking multiple times in quick succession
- Bulk operations generate many events
- System processes backlog after downtime
- High-frequency updates to popular aggregates

Without deduplication, you waste resources rebuilding the same aggregate repeatedly when only the final state matters for projections.

---

## Complete Implementation Example

### Command-Handler: Publish Lightweight Notification

```java
@Component
@Log4j2
public class BookingEventHandler extends EventHandler {

  private final StreamBridge streamBridge;

  @Override
  public void handle(List<EventEntity> eventEntityList) {
    final List<Event> events = this.eventMapper.toEventList(eventEntityList);

    for (Event event : events) {
      // Publish lightweight notification to Kafka
      EventNotification notification = EventNotification.builder()
          .aggregateId(event.getAggregateId())
          .aggregateType(event.getAggregateType())
          .eventType(event.getEventType())
          .aggregateVersion(event.getAggregateVersion())
          .timestamp(event.getTimestamp())
          .build();

      Message<EventNotification> message = MessageBuilder
          .withPayload(notification)
          .setHeader(KafkaHeaders.KEY, event.getAggregateId().toString())
          .build();
          
      streamBridge.send(KAFKA_BOOKING_NOTIFICATIONS, message);
      
      log.debug("Published notification for aggregate {} event {}", 
          event.getAggregateId(), event.getEventType());
    }
  }
}
```

### Query-Handler: Optimized Batch Consumer with Deduplication

```java
@Component
@Log4j2
@RequiredArgsConstructor
public class OptimizedBookingEventConsumer {

    private final List<ProjectionHandler> projectionHandlers;
    private final EventStoreService eventStoreService;
    private final EventMapper eventMapper;
    private final EventStoreConfigurationProperties eventStoreConfigurationProperties;

    /**
     * Batch consumer with deduplication.
     * Processes each aggregate only once per batch, using the latest version.
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
            
            int savedRebuilds = notifications.size() - latestByAggregate.size();
            log.info("Deduplicated to {} unique aggregates (saved {} rebuilds)", 
                latestByAggregate.size(), savedRebuilds);
            
            // Process each unique aggregate once in parallel
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

    /**
     * Retrieves aggregate from event store with snapshot optimization.
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
            // Load only events AFTER the snapshot - this is the key optimization!
            List<Event> events = retrieveEvents(aggregateId, aggregate.getAggregateVersion());
            aggregate.reproduceFromEvents(events);
            
            log.debug("Rebuilt aggregate {} from snapshot at version {} + {} new events",
                aggregateId, aggregate.getAggregateVersion(), events.size());
            
            return aggregate;
        });
    }

    protected Aggregate createAndReproduceAggregate(UUID aggregateId) {
        log.info("Snapshot not found for Aggregate ID: {}. Reconstituting from all events.", 
            aggregateId);
        
        Aggregate aggregate = AggregateFactory.newInstance(
            AggregateType.BOOKING_ORDER.getClazz(), 
            aggregateId
        );
        
        List<Event> events = retrieveEvents(aggregateId, null);
        aggregate.reproduceFromEvents(events);
        
        log.debug("Rebuilt aggregate {} from {} events", aggregateId, events.size());
        
        return aggregate;
    }

    protected List<Event> retrieveEvents(UUID aggregateId, Long fromVersion) {
        return eventMapper.toEventList(
            eventStoreService.retrieveEventsByAggregateId(aggregateId, fromVersion, null)
        );
    }
}
```

### Projection Handler with Version Checking

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
        
        // Check if we already have this version or newer (idempotency + out-of-order protection)
        Optional<BookingEntity> existing = repository.findById(booking.getBookingId());
        
        if (existing.isPresent()) {
            Integer currentVersion = existing.get().getVersion();
            Long newVersion = booking.getAggregateVersion();
            
            if (currentVersion >= newVersion) {
                log.debug("Skipping projection update - already at version {} (new: {})",
                    currentVersion, newVersion);
                return;
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

### Configuration

```yaml
# Kafka Configuration with Batch Processing
spring:
  cloud:
    stream:
      bindings:
        bookingEventBatchConsumer-in-0:
          destination: booking-notifications  # Lightweight notifications
          group: booking-query-handler
          consumer:
            batch-mode: true  # Enable batch processing
            max-attempts: 3
            back-off-initial-interval: 1000
      kafka:
        bindings:
          bookingEventBatchConsumer-in-0:
            consumer:
              batch-size: 100  # Process up to 100 messages at once
              batch-timeout: 1000  # Or wait max 1 second
        consumer:
          enable-auto-commit: false  # Manual commit after successful projection
          auto-offset-reset: earliest
          max-poll-records: 100  # Fetch up to 100 records per poll

# Event Store Configuration (for snapshot optimization)
spring:
  datasource:
    eventstore:
      url: jdbc:postgresql://localhost:5432/eventstore_db
      username: ${EVENTSTORE_USER:eventstore_user}
      password: ${EVENTSTORE_PASSWORD:eventstore_pass}

# Snapshot Configuration
eventstore:
  snapshot:
    booking-order:
      enabled: true
      frequency: 100  # Create snapshot every 100 events
```

---

## Performance Comparison with Optimizations

### Scenario 1: Adding a New Projection

**Aggregate State:**
- 100,000 bookings
- Average 50 events per booking
- Total: 5,000,000 events
- Snapshots every 100 events (average snapshot at event 25)

| Approach | Events Processed | Time to Build | Notes |
|----------|-----------------|---------------|-------|
| **Pure Kafka** | 5,000,000 | ~8-10 hours | Must process all events from Kafka |
| **Hybrid (Naive)** | ~2,500,000 | ~4-5 hours | Snapshot optimization but no deduplication |
| **Hybrid (Optimized)** | ~2,500,000 | ~30-45 minutes | Snapshot + batch deduplication ✅ |
| **Pure Event Store** | ~2,500,000 | ~30-45 minutes | Same as optimized hybrid |

### Scenario 2: Single Aggregate with Multiple Updates

**Aggregate State:**
- Booking with 1,000 events
- Snapshot at event 900
- 100 new events arrive in quick succession

| Approach | Rebuilds | Events Processed | Latency | Notes |
|----------|----------|-----------------|---------|-------|
| **Pure Kafka (Naive)** | 100 | 100 | ~1 second | Process each event individually |
| **Hybrid (Naive)** | 100 | 10,000 (100×100) | ~5 seconds | Rebuild from snapshot 100 times ❌ |
| **Hybrid (Optimized)** | 1 | 100 | ~50ms | Deduplicate, rebuild once ✅ |
| **Pure Event Store** | 1 | 100 | ~50ms | Same as optimized hybrid |

### Scenario 3: Bulk Operation

**Operation:**
- Update 1,000 bookings
- Each booking generates 5 events
- Total: 5,000 Kafka messages

| Approach | Rebuilds | Projection Updates | Time | Notes |
|----------|----------|-------------------|------|-------|
| **Hybrid (Naive)** | 5,000 | 5,000 | ~4 minutes | No deduplication ❌ |
| **Hybrid (Optimized, batch=100)** | 1,000 | 1,000 | ~50 seconds | Deduplicate per batch ✅ |
| **Hybrid (Optimized, batch=1000)** | 1,000 | 1,000 | ~50 seconds | Deduplicate entire operation ✅ |

### Key Takeaway

**Batch deduplication is CRITICAL** for the hybrid approach. Without it, you lose most of the performance benefits of snapshot optimization when processing multiple events for the same aggregate.

---

## Performance Comparison

### ✅ Use Hybrid Approach When:

- You need multiple services to consume events
- You want Kafka's pub/sub benefits
- You need to add new projections frequently
- Your aggregates have many events (>100 per aggregate)
- Query-handler can access event store database
- You want fast projection rebuilding

### ❌ Don't Use Hybrid Approach When:

- Single application (use pure Event Store Subscription)
- Query-handler cannot access event store (use pure Kafka with snapshots in Kafka)
- Aggregates are tiny (<10 events each)
- You never add new projections
- Latency is critical (every millisecond matters)

## Trade-offs

### Pros
- ✅ Kafka's decoupling and pub/sub benefits
- ✅ Full snapshot optimization
- ✅ Fast projection rebuilding
- ✅ Small Kafka messages
- ✅ Event store as source of truth
- ✅ Multiple event consumers supported
- ✅ **Batch deduplication prevents redundant rebuilds**
- ✅ **Efficient handling of high-frequency updates**

### Cons
- ⚠️ Query-handler needs event store access
- ⚠️ Slightly higher latency per event (50ms vs 10ms) - mitigated by batching
- ⚠️ Two data sources to manage (Kafka + Event Store)
- ⚠️ More complex than pure Event Store Subscription
- ⚠️ Requires careful batch size tuning

## Conclusion

The **Hybrid Kafka + Snapshot Optimization** approach is the sweet spot for microservices architectures that need:
1. Kafka's pub/sub and decoupling benefits
2. Fast projection rebuilding with snapshot optimization

It answers your question: **Yes, you can use Kafka AND take advantage of snapshot logic!**

The key insight is to use Kafka for **notifications** (what changed) rather than **data** (the full event), then query the event store for the actual aggregate state with snapshot optimization.

This is the recommended approach for microservices that want both Kafka's benefits and performance optimization.
