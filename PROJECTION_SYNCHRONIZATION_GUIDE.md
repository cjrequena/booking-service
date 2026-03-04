# Projection Synchronization Guide

## Overview

This document explains how projections are synchronized between the Command Handler (write side) and Query Handler (read side) in the Booking Service CQRS architecture.

---

## Architecture Approaches

The system supports two projection synchronization strategies, controlled by the `projections.handlers.enabled` configuration property.

### Strategy 1: Query-Handler Pulls Events (Recommended) ✅

**Configuration:**
```yaml
# booking-command-handler/src/main/resources/application.yml
projections:
  handlers:
    enabled: false  # Command handler does NOT update projections

# booking-query-handler/src/main/resources/application.yml
projections:
  handlers:
    enabled: true   # Query handler DOES update projections

event-store:
  polling:
    enabled: true
    interval: 5000      # Poll every 5 seconds
    batch-size: 100     # Process 100 events per batch
```

**Flow Diagram:**
```
┌─────────────────────────────────────────────────────────────┐
│                   Command-Handler                           │
│                  (Pure Command Side)                        │
├─────────────────────────────────────────────────────────────┤
│  1. Receive command                                         │
│  2. Update aggregate                                        │
│  3. Persist events to Event Store ✅                        │
│  4. Return response to client                               │
│  5. Done! (No projection logic)                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ Events stored in
                            ▼
                  ┌───────────────────┐
                  │   Event Store     │
                  │   (PostgreSQL)    │
                  └───────────────────┘
                            ▲
                            │ Polls for new events
                            │
┌─────────────────────────────────────────────────────────────┐
│                   Query-Handler                             │
│                  (Pure Query Side)                          │
├─────────────────────────────────────────────────────────────┤
│  1. ScheduledEventHandlerService polls Event Store          │
│  2. Fetches new events (batch processing)                   │
│  3. BookingEventHandler processes events                    │
│  4. Rebuilds aggregate from events                          │
│  5. Updates MongoDB projection                              │
│  6. Updates PostgreSQL projection (optional)                │
│  7. Invalidates cache                                       │
│  8. Exposes query endpoints                                 │
└─────────────────────────────────────────────────────────────┘
```

**Benefits:**
- ✅ Pure separation of concerns
- ✅ Query-Handler controls its own projection updates
- ✅ No direct coupling between services
- ✅ Easier to scale query handlers independently
- ✅ Multiple query handlers can poll the same Event Store
- ✅ Query handlers can be stopped/started without affecting writes
- ✅ Better fault isolation

**Use Cases:**
- Production environments
- Multiple query handlers with different projections
- Independent scaling requirements
- High availability setups

---

### Strategy 2: Command-Handler Pushes Projections

**Configuration:**
```yaml
# booking-command-handler/src/main/resources/application.yml
projections:
  handlers:
    enabled: true   # Command handler DOES update projections

event-store:
  polling:
    enabled: true
    interval: 5000
    batch-size: 100

# booking-query-handler/src/main/resources/application.yml
projections:
  handlers:
    enabled: false  # Query handler does NOT update projections
```

**Flow Diagram:**
```
┌─────────────────────────────────────────────────────────────┐
│                   Command-Handler                           │
│            (Command + Projection Updates)                   │
├─────────────────────────────────────────────────────────────┤
│  1. Receive command                                         │
│  2. Update aggregate                                        │
│  3. Persist events to Event Store ✅                        │
│  4. Return response to client                               │
│     ┌───────────────────────────────────────┐              │
│     │ Background Process:                   │              │
│     │ 5. ScheduledEventHandlerService polls │              │
│     │ 6. BookingEventHandler processes      │              │
│     │ 7. Updates MongoDB projection         │              │
│     │ 8. Updates PostgreSQL projection      │              │
│     └───────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ Projections updated
                            ▼
                  ┌───────────────────┐
                  │   MongoDB         │
                  │   (Projections)   │
                  └───────────────────┘
                            ▲
                            │ Reads from
                            │
┌─────────────────────────────────────────────────────────────┐
│                   Query-Handler                             │
│                  (Pure Query Side)                          │
├─────────────────────────────────────────────────────────────┤
│  1. Receives query requests                                 │
│  2. Reads from MongoDB projection                           │
│  3. Returns results                                         │
└─────────────────────────────────────────────────────────────┘
```

**Benefits:**
- ✅ Immediate projection updates
- ✅ Simpler query-handler implementation
- ✅ Centralized projection logic
- ✅ Single point of projection management

**Drawbacks:**
- ❌ Command handler has additional responsibility
- ❌ Tighter coupling between write and read sides
- ❌ Harder to scale independently
- ❌ Command handler must have access to projection databases

**Use Cases:**
- Development environments
- Simple deployments
- Single query handler scenarios
- When immediate consistency is preferred

---

## Kafka Usage (Optional)

**Important:** Kafka is NOT used for internal projection synchronization. It's only used for publishing events to external boundaries.

### When to Use Kafka

Kafka should be used when you need to:
- Publish events to external microservices
- Integrate with third-party systems
- Implement event-driven workflows across services
- Maintain event logs for analytics
- Enable event replay for external consumers

### Kafka Configuration

**Command Handler (Publisher):**
```yaml
spring:
  cloud:
    stream:
      bindings:
        booking-events-out:
          destination: booking-events
          content-type: application/json
      kafka:
        binder:
          brokers: localhost:9092
```

**External Consumer:**
```yaml
spring:
  cloud:
    stream:
      bindings:
        booking-events-in:
          destination: booking-events
          group: external-service
          content-type: application/json
```

---

## Implementation Details

### Scheduled Event Handler Service

**Location:** Can be in either Command Handler or Query Handler depending on configuration

**Implementation:**
```java
@Service
@EnableScheduling
@ConditionalOnProperty(name = "projections.handlers.enabled", havingValue = "true")
public class ScheduledEventHandlerService {
    
    private final EventStoreRepository eventStoreRepository;
    private final BookingEventHandler bookingEventHandler;
    private final LastProcessedEventTracker tracker;
    
    @Value("${event-store.polling.batch-size:100}")
    private int batchSize;
    
    @Scheduled(fixedDelayString = "${event-store.polling.interval:5000}")
    public void pollAndProcessEvents() {
        try {
            // 1. Get last processed event ID
            Long lastProcessedId = tracker.getLastProcessedEventId();
            
            // 2. Fetch new events from Event Store
            List<EventEntity> newEvents = eventStoreRepository
                .findByIdGreaterThanOrderByIdAsc(
                    lastProcessedId,
                    PageRequest.of(0, batchSize)
                );
            
            // 3. Process each event
            for (EventEntity eventEntity : newEvents) {
                try {
                    bookingEventHandler.handle(eventEntity);
                    tracker.updateLastProcessedEventId(eventEntity.getId());
                } catch (Exception e) {
                    log.error("Failed to process event: {}", eventEntity.getId(), e);
                    // Implement retry logic or dead letter handling
                }
            }
            
            log.debug("Processed {} events", newEvents.size());
            
        } catch (Exception e) {
            log.error("Error during event polling", e);
        }
    }
}
```

### Booking Event Handler

```java
@Component
@ConditionalOnProperty(name = "projections.handlers.enabled", havingValue = "true")
public class BookingEventHandler {
    
    private final BookingProjectionService projectionService;
    private final EventMapper eventMapper;
    
    public void handle(EventEntity eventEntity) {
        // Convert EventEntity to domain Event
        Event event = eventMapper.toEvent(eventEntity);
        
        // Route to appropriate handler based on event type
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
            default:
                log.warn("Unknown event type: {}", event.getEventType());
        }
    }
}
```

### Booking Projection Service

```java
@Service
@ConditionalOnProperty(name = "projections.handlers.enabled", havingValue = "true")
public class BookingProjectionService {
    
    private final BookingMongoRepository mongoRepository;
    private final BookingPostgresRepository postgresRepository; // Optional
    private final CacheManager cacheManager;
    
    @Transactional
    public void handleBookingCreated(BookingCreatedEvent event) {
        // Create new projection
        BookingEntity entity = BookingEntity.builder()
            .bookingId(event.getAggregateId())
            .bookingReference(event.getData().bookingReference())
            .status(event.getData().status())
            .paxes(mapPaxes(event.getData().paxes()))
            .leadPaxId(event.getData().leadPaxId())
            .products(mapProducts(event.getData().products()))
            .metadata(event.getData().metadata())
            .createdAt(LocalDateTime.now())
            .version(event.getAggregateVersion())
            .build();
        
        // Save to MongoDB
        mongoRepository.save(entity).subscribe();
        
        // Optionally save to PostgreSQL
        if (postgresRepository != null) {
            postgresRepository.save(entity);
        }
        
        log.info("Created projection for booking: {}", entity.getBookingId());
    }
    
    @Transactional
    public void handleBookingPlaced(BookingPlacedEvent event) {
        // Update existing projection
        mongoRepository.findByBookingId(event.getAggregateId())
            .flatMap(entity -> {
                entity.setStatus(BookingStatus.PLACED);
                entity.setUpdatedAt(LocalDateTime.now());
                entity.setVersion(event.getAggregateVersion());
                
                // Invalidate cache
                evictCache(entity.getBookingId());
                
                return mongoRepository.save(entity);
            })
            .subscribe();
        
        log.info("Updated projection for booking: {}", event.getAggregateId());
    }
    
    private void evictCache(UUID bookingId) {
        Cache cache = cacheManager.getCache("bookings");
        if (cache != null) {
            cache.evict(bookingId);
        }
    }
}
```

---

## Configuration Examples

### Development Environment

**Use Strategy 2** for simpler setup:

```yaml
# booking-command-handler
projections:
  handlers:
    enabled: true

# booking-query-handler
projections:
  handlers:
    enabled: false
```

### Production Environment

**Use Strategy 1** for better separation:

```yaml
# booking-command-handler
projections:
  handlers:
    enabled: false

# booking-query-handler
projections:
  handlers:
    enabled: true
event-store:
  polling:
    enabled: true
    interval: 5000
    batch-size: 100
```

---

## Monitoring & Troubleshooting

### Health Checks

**Event Store Polling Health:**
```bash
curl http://localhost:8081/actuator/health/eventStorePolling
```

**Response:**
```json
{
  "status": "UP",
  "details": {
    "lastProcessedEventId": 12345,
    "lastPollTime": "2026-03-04T15:30:00Z",
    "eventsProcessedInLastBatch": 25,
    "consecutiveFailures": 0
  }
}
```

### Metrics

Available metrics:
- `event.store.polling.last.processed.id`
- `event.store.polling.batch.size`
- `event.store.polling.processing.time`
- `event.store.polling.errors`
- `projection.updates.success`
- `projection.updates.failure`

### Troubleshooting

**Problem:** Projections are not updating

**Solutions:**
1. Check `projections.handlers.enabled` configuration
2. Verify Event Store connectivity
3. Check ScheduledEventHandlerService logs
4. Verify last processed event ID is advancing
5. Check for exceptions in BookingEventHandler

**Problem:** Projection lag is increasing

**Solutions:**
1. Increase `event-store.polling.batch-size`
2. Decrease `event-store.polling.interval`
3. Scale query handlers horizontally
4. Optimize projection update logic
5. Add indexes to MongoDB collections

---

## Best Practices

1. **Use Strategy 1 in Production** - Better separation of concerns
2. **Monitor Projection Lag** - Track time between event creation and projection update
3. **Implement Idempotency** - Events may be processed multiple times
4. **Handle Failures Gracefully** - Implement retry logic and dead letter handling
5. **Cache Invalidation** - Always invalidate cache after projection updates
6. **Batch Processing** - Process events in batches for better performance
7. **Circuit Breaker** - Stop polling if too many consecutive failures
8. **Metrics & Alerts** - Monitor projection health and set up alerts

---

## Summary

| Aspect | Strategy 1 (Pull) | Strategy 2 (Push) |
|--------|-------------------|-------------------|
| **Separation of Concerns** | ✅ Excellent | ⚠️ Good |
| **Scalability** | ✅ Excellent | ⚠️ Limited |
| **Complexity** | ⚠️ Higher | ✅ Lower |
| **Consistency** | ⚠️ Eventual | ✅ Near Real-time |
| **Fault Isolation** | ✅ Excellent | ⚠️ Good |
| **Production Ready** | ✅ Yes | ⚠️ Simple scenarios |
| **Recommended For** | Production | Development |

**Recommendation:** Use Strategy 1 (Query-Handler Pulls) for production environments and Strategy 2 (Command-Handler Pushes) for development/testing.
