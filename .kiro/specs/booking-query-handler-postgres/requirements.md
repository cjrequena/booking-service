# Requirements Document: PostgreSQL Projection for Booking Query Handler

## Introduction

This document specifies the requirements for implementing a PostgreSQL-based projection in a new `booking-query-handler-postgres` module. This is a separate module from the existing `booking-query-handler` module (which uses MongoDB). The implementation follows Approach 1 (Event Store Subscription) from the PostgreSQL Projection Implementation Guide, which provides clean CQRS separation by moving event handling logic to the query-handler while maintaining snapshot optimization for efficient projection rebuilding.

**IMPORTANT: All code for this feature must be implemented in the `booking-query-handler-postgres` module, NOT in the existing `booking-query-handler` module.**

The system uses Event Sourcing with the es-core framework. The command-handler persists events to the Event Store, and the new `booking-query-handler-postgres` module polls the Event Store to rebuild aggregates and update the PostgreSQL projection. This architecture enables true CQRS separation, allows easy addition of new projections, and supports efficient projection rebuilding through snapshot optimization.

**Module Structure:**
- `booking-command-handler` - Processes commands and persists events to Event Store
- `booking-query-handler` - Existing module with MongoDB projection (unchanged)
- `booking-query-handler-postgres` - NEW module with PostgreSQL projection (this spec)
- `es-core` - Shared Event Sourcing framework

## Glossary

- **Event_Store**: PostgreSQL database containing the source of truth for all domain events
- **Query_Handler_Postgres**: NEW service module responsible for reading events and maintaining PostgreSQL projection
- **Query_Handler**: Existing service module with MongoDB projection (unchanged)
- **Command_Handler**: Service responsible for processing commands and persisting events
- **Projection**: Read-optimized view of data derived from events
- **Projection_Handler**: Component that updates a specific projection when events are processed
- **Aggregate**: Domain object reconstructed from events
- **Snapshot**: Cached state of an aggregate at a specific version to optimize rebuilding
- **Booking_Aggregate**: Root aggregate representing a booking with passengers and products
- **Event_Subscription**: Mechanism for polling new events from the Event Store
- **Hybrid_Schema**: Database schema combining normalized relational tables with JSONB columns
- **Product_Details**: JSONB column storing polymorphic product-specific data (Transfer, Activity, Hotel)
- **Flyway**: Database migration tool for versioned schema changes
- **JPA**: Java Persistence API for object-relational mapping
- **JSONB**: PostgreSQL binary JSON data type with indexing support

## Requirements

### Requirement 1: Event Store Subscription Configuration

**User Story:** As a system architect, I want the new Query_Handler_Postgres module to poll the Event_Store for new events, so that the PostgreSQL projection can be updated independently from the Command_Handler and the existing MongoDB query handler.

#### Acceptance Criteria

1. THE Query_Handler_Postgres module SHALL connect to the Event_Store database using a dedicated datasource configuration
2. WHEN the Query_Handler_Postgres starts, THE Event_Subscription SHALL wait for the configured initial delay before polling
3. THE Event_Subscription SHALL poll the Event_Store at the configured interval for new events
4. THE Event_Subscription SHALL track the last processed transaction ID and event ID to avoid reprocessing
5. WHEN new events are found, THE Event_Subscription SHALL retrieve all events for affected aggregates
6. THE Event_Subscription SHALL maintain a subscription name for tracking processing state

### Requirement 2: Projection Handler Architecture

**User Story:** As a developer, I want a unified interface for projection handlers, so that multiple projections can be updated consistently from the same events.

#### Acceptance Criteria

1. THE System SHALL define a ProjectionHandler interface with handle and getAggregateType methods
2. THE MongoDB_Projection_Handler SHALL implement the ProjectionHandler interface
3. THE PostgreSQL_Projection_Handler SHALL implement the ProjectionHandler interface
4. WHEN an aggregate is rebuilt from events, THE System SHALL invoke all registered ProjectionHandler instances
5. THE System SHALL filter ProjectionHandler instances by aggregate type before invocation
6. IF any ProjectionHandler fails, THEN THE System SHALL propagate the exception to trigger retry on next poll

### Requirement 3: PostgreSQL Database Schema

**User Story:** As a database administrator, I want a hybrid PostgreSQL schema with normalized tables and JSONB columns, so that I can balance relational integrity with flexibility for polymorphic data.

#### Acceptance Criteria

1. THE System SHALL create a booking table with columns for booking_id, booking_reference, status, lead_pax_id, created_at, updated_at, and version
2. THE System SHALL create a booking_pax table with columns for pax_id, booking_id, first_name, last_name, email, phone, age, document_type, document_number, and pax_type
3. THE System SHALL create a booking_product table with columns for product_id, booking_id, search_id, search_created_at, product_type, status, hash, paxes_ids, and product_details
4. THE booking_pax table SHALL have a foreign key constraint to booking with cascade delete
5. THE booking_product table SHALL have a foreign key constraint to booking with cascade delete
6. THE paxes_ids column SHALL use JSONB type to store an array of UUID values
7. THE product_details column SHALL use JSONB type to store polymorphic product-specific data
8. THE System SHALL create an index on booking_reference for unique constraint enforcement
9. THE System SHALL create indexes on booking status and lead_pax_id for query optimization
10. THE System SHALL create indexes on booking_pax email and booking_id for query optimization
11. THE System SHALL create indexes on booking_product booking_id and product_type for query optimization
12. THE System SHALL create a GIN index on product_details for JSONB query optimization

### Requirement 4: Database Migration Management

**User Story:** As a developer, I want database schema changes managed through Flyway migrations, so that schema evolution is versioned and repeatable.

#### Acceptance Criteria

1. THE System SHALL use Flyway for PostgreSQL schema migrations
2. THE System SHALL store migration scripts in the db/migration/postgresql directory
3. WHEN the Query_Handler starts, THE System SHALL execute pending Flyway migrations
4. THE System SHALL enable baseline-on-migrate for existing databases
5. THE System SHALL validate the schema matches the expected version before starting

### Requirement 5: JPA Entity Implementation

**User Story:** As a developer, I want JPA entities with JSONB support, so that I can persist and query booking data in PostgreSQL.

#### Acceptance Criteria

1. THE System SHALL define a BookingEntity class mapped to the booking table
2. THE System SHALL define a PaxEntity class mapped to the booking_pax table
3. THE System SHALL define a ProductEntity class mapped to the booking_product table
4. THE BookingEntity SHALL have a one-to-many relationship with PaxEntity using cascade all and orphan removal
5. THE BookingEntity SHALL have a one-to-many relationship with ProductEntity using cascade all and orphan removal
6. THE ProductEntity SHALL use Hypersistence Utils JsonBinaryType for the paxes_ids JSONB column
7. THE ProductEntity SHALL use Hypersistence Utils JsonBinaryType for the product_details JSONB column
8. THE BookingEntity SHALL use optimistic locking with a version field
9. THE BookingEntity SHALL use creation and update timestamps with automatic management
10. THE PaxEntity and ProductEntity SHALL use lazy fetching for the booking relationship

### Requirement 6: Aggregate to Entity Mapping

**User Story:** As a developer, I want mappers to convert domain aggregates to JPA entities, so that projection updates are decoupled from domain logic.

#### Acceptance Criteria

1. THE System SHALL define a mapper to convert Booking_Aggregate to BookingEntity
2. THE mapper SHALL convert all Pax value objects to PaxEntity instances
3. THE mapper SHALL convert all Product value objects to ProductEntity instances
4. WHEN mapping a Transfer product, THE mapper SHALL serialize transfer-specific fields to the product_details JSONB column
5. THE mapper SHALL preserve all product metadata fields including product_id, search_id, search_created_at, product_type, status, and paxes_ids
6. THE mapper SHALL establish bidirectional relationships between BookingEntity and child entities
7. THE mapper SHALL handle null values for optional fields like inbound_trip in transfers

### Requirement 7: PostgreSQL Projection Handler Implementation

**User Story:** As a developer, I want a PostgreSQL projection handler that updates the PostgreSQL database when events are processed, so that the PostgreSQL projection stays synchronized with the Event_Store.

#### Acceptance Criteria

1. THE PostgreSQL_Projection_Handler SHALL implement the ProjectionHandler interface
2. WHEN handle is invoked with a Booking_Aggregate, THE PostgreSQL_Projection_Handler SHALL convert the aggregate to a BookingEntity
3. THE PostgreSQL_Projection_Handler SHALL save the BookingEntity using the PostgreSQL repository
4. THE PostgreSQL_Projection_Handler SHALL return AggregateType BOOKING_ORDER from getAggregateType
5. THE PostgreSQL_Projection_Handler SHALL use transactional boundaries for database operations
6. IF the save operation fails, THEN THE PostgreSQL_Projection_Handler SHALL propagate the exception
7. THE PostgreSQL_Projection_Handler SHALL log successful projection updates at debug level

### Requirement 8: Repository Layer

**User Story:** As a developer, I want Spring Data JPA repositories with custom queries, so that I can efficiently query the PostgreSQL projection.

#### Acceptance Criteria

1. THE System SHALL define a BookingPostgresRepository extending JpaRepository
2. THE BookingPostgresRepository SHALL provide a method to find bookings by booking reference
3. THE BookingPostgresRepository SHALL provide a method to find bookings by status
4. THE BookingPostgresRepository SHALL provide a method to find bookings by lead passenger ID
5. THE BookingPostgresRepository SHALL provide a method to find bookings by passenger email
6. THE BookingPostgresRepository SHALL provide a method to find a booking with eagerly fetched paxes and products
7. THE System SHALL use the repository method with fetch joins to avoid N+1 query problems

### Requirement 9: Service Layer

**User Story:** As a developer, I want a service layer for PostgreSQL projection operations, so that business logic is separated from repository concerns.

#### Acceptance Criteria

1. THE System SHALL define a BookingPostgresProjectionService for read operations
2. THE BookingPostgresProjectionService SHALL provide a method to retrieve a booking by ID with details
3. THE BookingPostgresProjectionService SHALL provide a method to retrieve a booking by reference
4. THE BookingPostgresProjectionService SHALL provide a method to retrieve all bookings
5. WHEN a booking is not found, THE BookingPostgresProjectionService SHALL throw a BookingNotFoundException
6. THE BookingPostgresProjectionService SHALL use read-only transactions for query operations
7. THE BookingPostgresProjectionService SHALL cache booking queries using Spring Cache abstraction

### Requirement 10: Controller Endpoints

**User Story:** As an API consumer, I want REST endpoints to query the PostgreSQL projection, so that I can retrieve booking data from the relational database.

#### Acceptance Criteria

1. THE System SHALL expose a GET endpoint at /query-handler/api/bookings/postgres/{bookingId}
2. THE System SHALL expose a GET endpoint at /query-handler/api/bookings/postgres
3. THE System SHALL expose a GET endpoint at /query-handler/api/bookings/postgres/reference/{reference}
4. WHEN a booking is found, THE endpoints SHALL return HTTP 200 with the BookingEntity
5. WHEN a booking is not found, THE endpoints SHALL return HTTP 404 with an error message
6. THE endpoints SHALL use the BookingPostgresProjectionService for data retrieval
7. THE endpoints SHALL maintain the existing MongoDB endpoints without modification

### Requirement 11: Dual Datasource Configuration

**User Story:** As a system administrator, I want separate datasource configurations for the Event_Store and PostgreSQL projection, so that connection pools and settings can be optimized independently.

#### Acceptance Criteria

1. THE System SHALL configure an eventstore datasource for Event_Store access
2. THE System SHALL configure a postgresql datasource for projection storage
3. THE eventstore datasource SHALL use connection parameters from environment variables or defaults
4. THE postgresql datasource SHALL use connection parameters from environment variables or defaults
5. THE postgresql datasource SHALL configure HikariCP with maximum pool size of 20 and minimum idle of 10
6. THE System SHALL configure JPA to use PostgreSQL dialect for the projection datasource
7. THE System SHALL configure JPA with ddl-auto set to validate to prevent automatic schema changes
8. THE System SHALL configure Flyway to use the postgresql datasource for migrations

### Requirement 12: Snapshot Optimization Support

**User Story:** As a system architect, I want the Query_Handler to leverage snapshot optimization when rebuilding aggregates, so that projection rebuilding is fast even for aggregates with thousands of events.

#### Acceptance Criteria

1. WHEN rebuilding an aggregate, THE System SHALL check if snapshot configuration is enabled for the aggregate type
2. IF snapshots are enabled, THEN THE System SHALL attempt to retrieve the latest snapshot for the aggregate
3. WHEN a snapshot is found, THE System SHALL load only events after the snapshot version
4. WHEN a snapshot is not found, THE System SHALL load all events from the beginning
5. THE System SHALL reproduce the aggregate state by applying events to the snapshot or new instance
6. THE System SHALL use the same snapshot optimization logic as the Command_Handler
7. FOR ALL aggregates with snapshots, rebuilding SHALL process only events after the snapshot version

### Requirement 13: Error Handling and Retry

**User Story:** As a system operator, I want robust error handling with automatic retry, so that transient failures do not cause permanent projection inconsistencies.

#### Acceptance Criteria

1. WHEN a projection update fails, THE System SHALL propagate the exception to the event handler
2. WHEN the event handler encounters an exception, THE System SHALL not update the subscription offset
3. WHEN the next polling interval occurs, THE System SHALL retry processing the failed events
4. THE System SHALL log projection update failures at error level with aggregate ID and exception details
5. THE System SHALL log successful projection updates at debug level to avoid log noise
6. IF all ProjectionHandler instances succeed, THEN THE System SHALL update the subscription offset
7. THE System SHALL process events in a transactional boundary to ensure atomicity

### Requirement 14: Product Details JSONB Structure

**User Story:** As a developer, I want a well-defined JSONB structure for product_details, so that polymorphic product data can be stored and queried consistently.

#### Acceptance Criteria

1. FOR ALL Transfer products, THE product_details JSONB SHALL contain origin, destination, outbound_trip, inbound_trip, and price fields
2. THE origin and destination fields SHALL contain latitude, longitude, iata_code, and full_address
3. THE outbound_trip field SHALL contain trip_id, pickup_datetime, transfer_type, and vehicle
4. THE vehicle field SHALL contain vehicle_id, type, model, capacity, max_bags, and max_paxes
5. THE price field SHALL contain service_type, currency, total_amount, subtotal_amount, fees_and_taxes, and pax_prices
6. THE inbound_trip field SHALL be null for one-way transfers
7. THE System SHALL preserve the complete product structure when mapping from aggregate to entity

### Requirement 15: Projection Consistency Verification

**User Story:** As a quality assurance engineer, I want the ability to verify projection consistency, so that I can detect and correct any divergence between MongoDB and PostgreSQL projections.

#### Acceptance Criteria

1. THE System SHALL provide a method to retrieve a booking from both MongoDB and PostgreSQL projections
2. THE System SHALL provide a method to compare booking data between projections
3. WHEN comparing projections, THE System SHALL check booking_id, booking_reference, status, and lead_pax_id match
4. WHEN comparing projections, THE System SHALL check the number of paxes and products match
5. WHEN comparing projections, THE System SHALL check pax details match between projections
6. WHEN comparing projections, THE System SHALL check product metadata matches between projections
7. IF projections diverge, THEN THE System SHALL log the differences at warning level

### Requirement 16: Performance Optimization

**User Story:** As a performance engineer, I want optimized query patterns and indexing, so that the PostgreSQL projection provides fast query response times.

#### Acceptance Criteria

1. WHEN retrieving a booking with details, THE System SHALL use a single query with fetch joins
2. THE System SHALL avoid N+1 query problems by eagerly fetching required associations
3. THE System SHALL use database indexes for all common query patterns
4. THE System SHALL use GIN indexes for JSONB column queries
5. THE System SHALL use connection pooling with appropriate pool size for expected load
6. THE System SHALL use read-only transactions for query operations to optimize database locking
7. THE System SHALL cache frequently accessed bookings using Spring Cache abstraction

### Requirement 17: Parallel Event Processing

**User Story:** As a system architect, I want parallel processing of events for different aggregates, so that projection updates can scale with event volume.

#### Acceptance Criteria

1. WHEN processing a batch of events, THE System SHALL identify unique aggregate IDs
2. THE System SHALL process each unique aggregate in parallel using parallel streams
3. THE System SHALL rebuild each aggregate independently without shared state
4. THE System SHALL update all projections for each aggregate before moving to the next
5. IF processing one aggregate fails, THEN THE System SHALL not affect processing of other aggregates in the same batch
6. THE System SHALL use the configured thread pool for parallel processing
7. THE System SHALL log the number of events and unique aggregates processed in each batch

### Requirement 18: Idempotency

**User Story:** As a system architect, I want idempotent projection updates, so that reprocessing events does not cause data corruption.

#### Acceptance Criteria

1. WHEN saving a BookingEntity, THE System SHALL use the booking_id as the primary key
2. IF a booking already exists, THEN THE System SHALL update the existing record
3. THE System SHALL use optimistic locking to detect concurrent modifications
4. WHEN a version conflict occurs, THE System SHALL throw an exception to trigger retry
5. THE System SHALL delete orphaned child entities when updating parent entities
6. THE System SHALL use cascade operations to maintain referential integrity
7. FOR ALL projection updates, applying the same events multiple times SHALL produce the same final state

### Requirement 19: Monitoring and Observability

**User Story:** As a system operator, I want comprehensive logging and metrics, so that I can monitor projection health and troubleshoot issues.

#### Acceptance Criteria

1. THE System SHALL log the number of events processed in each polling cycle
2. THE System SHALL log the number of unique aggregates processed in each polling cycle
3. THE System SHALL log the time taken to process each batch of events
4. THE System SHALL log projection handler invocations at debug level
5. THE System SHALL log projection update failures at error level with full stack traces
6. THE System SHALL log subscription offset updates at debug level
7. THE System SHALL expose metrics for event processing rate, projection update latency, and error rate

### Requirement 20: Configuration Externalization

**User Story:** As a system administrator, I want all configuration externalized through environment variables or configuration files, so that the system can be deployed in different environments without code changes.

#### Acceptance Criteria

1. THE System SHALL read Event_Store connection parameters from environment variables with fallback defaults
2. THE System SHALL read PostgreSQL connection parameters from environment variables with fallback defaults
3. THE System SHALL read event subscription polling interval from configuration
4. THE System SHALL read event subscription initial delay from configuration
5. THE System SHALL read snapshot configuration from the Event_Store configuration properties
6. THE System SHALL read connection pool settings from configuration
7. THE System SHALL read Flyway migration settings from configuration
