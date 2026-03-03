# Implementation Plan: PostgreSQL Projection for Booking Query Handler

## ⚠️ CRITICAL: Module Location

**ALL code must be implemented in the `booking-query-handler-postgres` module.**

**DO NOT implement in `booking-query-handler` module (that's the MongoDB module).**

**Base package:** `com.cjrequena.sample.query.handler.postgres`

**Module path:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/`

## ⚠️ CRITICAL: Self-Contained Module

**The `booking-query-handler-postgres` module is completely self-contained:**

- It has its own domain models, enums (including AggregateType), and exceptions
- It does NOT import domain classes from `booking-command-handler` or `booking-query-handler` modules
- The only shared dependency is `es-core` for Event Sourcing infrastructure (Aggregate base class, Event, etc.)
- All imports must be from `com.cjrequena.sample.query.handler.postgres.*` or `com.cjrequena.sample.es.core.*`

**✅ COMPLETED: Domain Model Implementation (Task 7.2)**

All domain models have been successfully created in the postgres module:
- **Enums:** AggregateType, BookingStatus, ProductStatus, ProductType, TransferServiceType, TransferType
- **Exceptions:** DomainRuntimeException, InvalidArgumentException, PaxPriceException
- **Value Objects:** PaxVO, PaxPriceVO, LocationVO, ProductMetadataVO, ProductVO, VehicleVO, TripVO, TransferPriceVO, TransferVO
- **Aggregate:** Booking (simplified for query-side event replay)
- **Mapper:** BookingEntityMapper updated to use local postgres domain models (all imports corrected)

---

## Overview

This implementation plan breaks down the PostgreSQL projection feature into actionable coding tasks. The implementation creates a NEW `booking-query-handler-postgres` module that follows the Event Store Subscription approach, where the query-handler polls the Event Store for new events and updates the PostgreSQL projection. The system leverages snapshot optimization for efficient aggregate rebuilding and uses a hybrid PostgreSQL schema combining normalized tables with JSONB columns for polymorphic data.

**CRITICAL: All code must be implemented in the `booking-query-handler-postgres` module, NOT in the existing `booking-query-handler` module.**

**Module Location:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/`

**Base Package:** `com.cjrequena.sample.query.handler.postgres`

## Tasks

- [x] 1. Add PostgreSQL dependencies to pom.xml
  - Add PostgreSQL driver dependency
  - Add Spring Data JPA dependency
  - Add Hypersistence Utils for JSONB support (version 3.7.0)
  - Add Flyway core and PostgreSQL dependencies
  - Add Caffeine cache dependency
  - **Location:** `booking-query-handler-postgres/pom.xml`
  - _Requirements: 4.1, 5.1_

- [x] 2. Create Flyway database migration scripts
  - [x] 2.1 Create V1__create_booking_schema.sql migration
    - Create booking table with all columns (booking_id, booking_reference, status, lead_pax_id, created_at, updated_at, version)
    - Create booking_pax table with all columns and foreign key to booking with CASCADE DELETE
    - Create booking_product table with JSONB columns (paxes_ids, product_details) and foreign key to booking with CASCADE DELETE
    - **Location:** `booking-query-handler-postgres/src/main/resources/db/migration/postgresql/`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 4.2, 4.3_
  
  - [x] 2.2 Create V2__add_indexes.sql migration
    - Create unique index on booking_reference
    - Create indexes on booking status and lead_pax_id
    - Create indexes on booking_pax email and booking_id
    - Create indexes on booking_product booking_id and product_type
    - Create GIN index on product_details JSONB column
    - **Location:** `booking-query-handler-postgres/src/main/resources/db/migration/postgresql/`
    - _Requirements: 3.8, 3.9, 3.10, 3.11, 3.12, 16.3, 16.4_

- [x] 3. Implement PostgreSQL JPA entities
  - [x] 3.1 Create BookingEntity class
    - Map to booking table with @Entity and @Table annotations
    - Add all fields: bookingId (UUID, @Id), bookingReference, status (enum), leadPaxId, createdAt, updatedAt, version
    - Add @OneToMany relationships to PaxEntity and ProductEntity with cascade ALL and orphan removal
    - Add @CreationTimestamp and @UpdateTimestamp for automatic timestamp management
    - Add @Version for optimistic locking
    - Add helper methods addPax() and addProduct() for bidirectional relationships
    - Use Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
    - _Requirements: 5.1, 5.4, 5.5, 5.8, 5.9_
  
  - [x] 3.2 Create PaxEntity class
    - Map to booking_pax table with @Entity and @Table annotations
    - Add all fields: paxId (UUID, @Id), firstName, lastName, email, phone, age, documentType, documentNumber, paxType
    - Add @ManyToOne relationship to BookingEntity with LAZY fetching
    - Use @ToString.Exclude on booking field to avoid circular references
    - Use Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
    - _Requirements: 5.2, 5.10_
  
  - [x] 3.3 Create ProductEntity class
    - Map to booking_product table with @Entity and @Table annotations
    - Add all fields: productId (UUID, @Id), searchId, searchCreatedAt, productType (enum), status (enum), hash
    - Add paxesIds field with @Type(JsonBinaryType.class) and columnDefinition="jsonb"
    - Add productDetails field (Map<String, Object>) with @Type(JsonBinaryType.class) and columnDefinition="jsonb"
    - Add @ManyToOne relationship to BookingEntity with LAZY fetching
    - Use @ToString.Exclude on booking field
    - Use Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
    - _Requirements: 5.3, 5.6, 5.7, 5.10_

- [x] 4. Create BookingPostgresRepository interface
  - Extend JpaRepository<BookingEntity, UUID>
  - Add findByBookingReference method returning Optional<BookingEntity>
  - Add findByStatus method returning List<BookingEntity>
  - Add findByLeadPaxId method returning List<BookingEntity>
  - Add findByPaxEmail method with @Query joining paxes
  - Add findByIdWithDetails method with @Query using LEFT JOIN FETCH for paxes and products
  - **Location:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/persistence/repository/BookingPostgresRepository.java`
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 16.1, 16.2_

- [x] 5. Implement BookingEntityMapper component
  - **Location:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/mapper/BookingEntityMapper.java`
  - **IMPORTANT:** Use domain models from `com.cjrequena.sample.query.handler.postgres.domain.*` (NOT from command-handler)
  - **DEPENDENCY:** Requires task 7.2 (domain models) to be completed first
  - **COMPLETED:** All imports updated to use local postgres domain models (task 7.2 dependency resolved)
  - [x] 5.1 Create toEntity method to convert Booking aggregate to BookingEntity
    - Import Booking from `com.cjrequena.sample.query.handler.postgres.domain.aggregate.Booking`
    - Map all booking fields (bookingId, bookingReference, status, leadPaxId)
    - Iterate through paxes and convert each to PaxEntity using toPaxEntity method
    - Iterate through products and convert each to ProductEntity using toProductEntity method
    - Use addPax() and addProduct() helper methods to establish bidirectional relationships
    - _Requirements: 6.1, 6.2, 6.3, 6.6_
  
  - [x] 5.2 Create toPaxEntity method to convert Pax to PaxEntity
    - Import PaxVO from `com.cjrequena.sample.query.handler.postgres.domain.vo.PaxVO`
    - Map all pax fields (paxId, firstName, lastName, email, phone, age, documentType, documentNumber, paxType)
    - _Requirements: 6.2_
  
  - [x] 5.3 Create toProductEntity method to convert Product to ProductEntity
    - Import ProductVO and TransferVO from `com.cjrequena.sample.query.handler.postgres.domain.vo.*`
    - Map product metadata fields (productId, searchId, searchCreatedAt, productType, status, hash, paxesIds)
    - Check if product is instance of Transfer and call serializeTransfer method
    - Set productDetails field with serialized data
    - _Requirements: 6.3, 6.4, 6.5_
  
  - [x] 5.4 Create serializeTransfer method to convert Transfer to Map for JSONB
    - Serialize origin (latitude, longitude, iataCode, fullAddress)
    - Serialize destination (latitude, longitude, iataCode, fullAddress)
    - Serialize outboundTrip (tripId, pickupDatetime, transferType, vehicle details)
    - Serialize inboundTrip if not null (handle one-way transfers)
    - Serialize price (serviceType, currency, totalAmount, subtotalAmount, feesAndTaxes, paxPrices)
    - Return Map<String, Object> for JSONB storage
    - _Requirements: 6.4, 6.7, 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7_

- [x] 6. Implement BookingPostgresProjectionService
  - Add @Service and @Transactional(readOnly = true) annotations
  - Inject BookingPostgresRepository
  - [x] 6.1 Create retrieveById method
    - Call repository.findByIdWithDetails with bookingId
    - Throw BookingNotFoundException if not found
    - Add @Cacheable annotation with value="postgres-bookings" and key="#bookingId"
    - _Requirements: 9.2, 9.5, 9.6, 9.7, 16.7_
  
  - [x] 6.2 Create retrieveByReference method
    - Call repository.findByBookingReference with bookingReference
    - Throw BookingNotFoundException if not found
    - _Requirements: 9.3, 9.5_
  
  - [x] 6.3 Create retrieveAll method
    - Call repository.findAll and return list
    - _Requirements: 9.4_
  
  - [x] 6.4 Create save method with @Transactional annotation
    - Add @CacheEvict annotation with value="postgres-bookings" and key="#entity.bookingId"
    - Call repository.save with entity
    - _Requirements: 7.3, 16.7_

- [x] 7. Create BookingNotFoundException exception class
  - Extend DomainRuntimeException
  - Add constructor accepting message string
  - Place in domain.exception package
  - _Requirements: 9.5_

- [x] 7.1 Create AggregateType enum
  - Define BOOKING_ORDER constant with type "Booking"
  - Add getType() method returning String
  - Place in domain.enums package
  - **Location:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/domain/enums/AggregateType.java`
  - **IMPORTANT:** Use local AggregateType from postgres module, NOT from command-handler module
  - _Requirements: 2.1_

- [x] 7.2 Create domain model classes (Booking aggregate and value objects)
  - **CRITICAL:** Copy domain models from command-handler to postgres module
  - Create Booking aggregate extending es-core Aggregate in domain.aggregate package
  - Create all value objects (PaxVO, ProductVO, TransferVO, LocationVO, TripVO, VehicleVO, TransferPriceVO, etc.) in domain.vo package
  - Update package declarations to `com.cjrequena.sample.query.handler.postgres.domain.*`
  - Keep the same structure and fields as command-handler for event replay compatibility
  - **Location:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/domain/`
  - **IMPORTANT:** These are LOCAL copies - do NOT import from command-handler module
  - **COMPLETED:** All domain models created including:
    - Enums: AggregateType, BookingStatus, ProductStatus, ProductType, TransferServiceType, TransferType
    - Exceptions: DomainRuntimeException, InvalidArgumentException, PaxPriceException
    - Value Objects: PaxVO, PaxPriceVO, LocationVO, ProductMetadataVO, ProductVO, VehicleVO, TripVO, TransferPriceVO, TransferVO
    - Aggregate: Booking
  - **COMPLETED:** BookingEntityMapper updated to use local postgres domain models (all imports corrected)
  - _Requirements: 2.1, 6.1, 6.2, 6.3, 6.4, 7.2_

- [x] 8. Create ProjectionHandler interface
  - Define handle method accepting Aggregate parameter
  - Define getAggregateType method returning AggregateType (from postgres module)
  - Place in service.projection package
  - **IMPORTANT:** Import AggregateType from com.cjrequena.sample.query.handler.postgres.domain.enums
  - _Requirements: 2.1_

- [x] 9. Implement BookingPostgresProjectionHandler
  - Add @Component annotation
  - Implement ProjectionHandler interface
  - Inject BookingPostgresProjectionService and BookingEntityMapper
  - **IMPORTANT:** Cast aggregate to local Booking class from `com.cjrequena.sample.query.handler.postgres.domain.aggregate.Booking`
  - [x] 9.1 Implement handle method
    - Add @Transactional annotation
    - Cast aggregate to Booking (from postgres module)
    - Convert booking to BookingEntity using mapper.toEntity
    - Save entity using postgresService.save
    - Log successful update at debug level
    - Catch and log exceptions at error level with aggregate ID, then rethrow
    - _Requirements: 7.1, 7.2, 7.3, 7.5, 7.6, 7.7, 13.4_
  
  - [x] 9.2 Implement getAggregateType method
    - Return AggregateType.BOOKING_ORDER (from postgres module)
    - _Requirements: 7.4_

- [x] 10. Create EventHandler base class in booking-query-handler-postgres module
  - **NOTE:** This task creates event handling infrastructure in the NEW `booking-query-handler-postgres` module only. The existing `booking-query-handler` (MongoDB) module remains UNCHANGED per the design document.
  - **NOTE:** Requirement 2.2 mentions MongoDB handler implementing ProjectionHandler, but this is out of scope for this implementation. The postgres module is self-contained and does not modify the MongoDB module.
  - **Location:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/service/event/EventHandler.java`
  - Add @Transactional annotation
  - Inject EventStoreService, EventMapper, and EventStoreConfigurationProperties
  - [x] 10.1 Create abstract handle method
    - Accept List<EventEntity> parameter
    - Subclasses will implement event processing logic
    - _Requirements: 2.1_
  
  - [x] 10.2 Create abstract getAggregateType method
    - Return AggregateType (from postgres module)
    - Subclasses will specify which aggregate type they handle
    - _Requirements: 2.1_
  
  - [x] 10.3 Create retrieveOrInstantiateAggregate method
    - Accept UUID aggregateId parameter
    - Check if snapshot is enabled for aggregate type
    - If enabled, call retrieveAggregateFromSnapshot, fallback to createAndReproduceAggregate
    - If disabled, call createAndReproduceAggregate directly
    - _Requirements: 12.1, 12.2_
  
  - [x] 10.4 Create retrieveAggregateFromSnapshot method
    - Accept UUID aggregateId parameter
    - Retrieve snapshot from Event Store using eventStoreService
    - If snapshot found, retrieve events after snapshot version and reproduce aggregate
    - Return Optional<Aggregate>
    - _Requirements: 12.3_
  
  - [x] 10.5 Create createAndReproduceAggregate method
    - Accept UUID aggregateId parameter
    - Create new aggregate instance using AggregateFactory
    - Retrieve all events from beginning using retrieveEvents
    - Reproduce aggregate from events
    - Return Aggregate
    - _Requirements: 12.4, 12.5_
  
  - [x] 10.6 Create retrieveEvents helper method
    - Accept UUID aggregateId and Long fromVersion parameters
    - Retrieve events from Event Store using eventStoreService
    - Convert EventEntity list to Event list using eventMapper
    - Return List<Event>
    - _Requirements: 1.5_

- [x] 11. Create BookingEventHandler in booking-query-handler-postgres module
  - **Location:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/service/event/BookingEventHandler.java`
  - Add @Component and @Transactional annotations
  - Extend EventHandler base class (from postgres module)
  - Inject List<ProjectionHandler> using constructor injection
  - [x] 11.1 Implement handle method
    - Convert EventEntity list to Event list using eventMapper
    - Extract unique aggregate IDs from events using parallelStream
    - For each unique aggregate ID:
      - Call retrieveOrInstantiateAggregate to rebuild aggregate from Event Store
      - Filter projectionHandlers by aggregate type
      - Invoke handle method on each matching ProjectionHandler
    - Ensure exceptions from handlers propagate to trigger retry
    - _Requirements: 2.4, 2.5, 2.6, 13.1, 13.2, 17.1, 17.2, 17.3_
  
  - [x] 11.2 Implement getAggregateType method
    - Return AggregateType.BOOKING_ORDER (from postgres module)
    - _Requirements: 2.1_

- [x] 11.3 Create EventMapper in booking-query-handler-postgres module
  - **Location:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/mapper/EventMapper.java`
  - Add @Component annotation
  - Create toEventList method to convert List<EventEntity> to List<Event>
  - Use ObjectMapper to deserialize event data from EventEntity
  - Handle different event types dynamically based on event_type field
  - _Requirements: 1.5_

- [x] 11.4 Create ScheduledEventHandlerService in booking-query-handler-postgres module
  - **Location:** `booking-query-handler-postgres/src/main/java/com/cjrequena/sample/query/handler/postgres/service/event/ScheduledEventHandlerService.java`
  - Add @Service and @Transactional annotations
  - Add @ConditionalOnProperty(name = "eventstore.subscription.enabled", havingValue = "true")
  - Inject EventStoreService, List<EventHandler>, and EventStoreConfigurationProperties
  - [x] 11.4.1 Create scheduled handler method
    - Add @Scheduled annotation with fixedDelayString and initialDelayString from config
    - Iterate through all eventHandlers and invoke handler method for each
    - _Requirements: 1.2, 1.3_
  
  - [x] 11.4.2 Create handler method for individual EventHandler
    - Add @Async annotation for parallel processing
    - Get subscription properties from configuration
    - Register new subscription if absent using eventStoreService
    - Retrieve and lock subscription offset using eventStoreService
    - If lock acquired:
      - Retrieve new events after last offset using eventStoreService
      - If events found, invoke eventHandler.handle with events
      - Update subscription offset with last processed event
    - If lock not acquired, log and skip this cycle
    - _Requirements: 1.1, 1.4, 1.6, 13.6, 17.1_

- [x] 12. Add PostgreSQL endpoints to BookingQueryController
  - Inject BookingPostgresProjectionService
  - [x] 12.1 Create GET endpoint /bookings/{bookingId}
    - Accept UUID bookingId as path variable
    - Call postgresService.retrieveById
    - Return ResponseEntity with BookingEntity and HTTP 200
    - Handle BookingNotFoundException and return HTTP 404
    - _Requirements: 10.1, 10.4, 10.5, 10.6_
  
  - [x] 12.2 Create GET endpoint /bookings
    - Call postgresService.retrieveAll
    - Return ResponseEntity with List<BookingEntity> and HTTP 200
    - _Requirements: 10.2, 10.4, 10.6_

- [x] 13. Configure dual datasources
  - [x] 13.1 Create DatasourceConfiguration class
    - Add @Configuration annotation
    - Create eventstoreDataSourceProperties bean with @ConfigurationProperties("spring.datasource.eventstore")
    - Create eventstoreDataSource bean with @Primary and @ConfigurationProperties("spring.datasource.eventstore.hikari")
    - Create postgresqlDataSourceProperties bean with @ConfigurationProperties("spring.datasource.postgresql")
    - Create postgresqlDataSource bean with @ConfigurationProperties("spring.datasource.postgresql.hikari")
    - Use HikariDataSource for both datasources
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_
  
  - [x] 13.2 Create PostgresqlJpaConfiguration class
    - Add @Configuration annotation
    - Add @EnableJpaRepositories with basePackages pointing to postgresql repository package
    - Set entityManagerFactoryRef to "postgresqlEntityManagerFactory"
    - Set transactionManagerRef to "postgresqlTransactionManager"
    - Create postgresqlEntityManagerFactory bean using EntityManagerFactoryBuilder
    - Set dataSource to postgresqlDataSource, packages to postgresql entity package, persistenceUnit to "postgresql"
    - Add JPA properties: hibernate.dialect=PostgreSQLDialect, hibernate.hbm2ddl.auto=validate, hibernate.jdbc.batch_size=20, hibernate.order_inserts=true, hibernate.order_updates=true
    - Create postgresqlTransactionManager bean using JpaTransactionManager
    - _Requirements: 11.6, 11.7, 16.5_
  
  - [x] 13.3 Create FlywayConfiguration class
    - Add @Configuration annotation
    - Create flyway bean using Flyway.configure() with postgresqlDataSource
    - Set locations to "classpath:db/migration/postgresql"
    - Set baselineOnMigrate to true, baselineVersion to "0", validateOnMigrate to true
    - Create flywayInitializer bean using FlywayMigrationInitializer
    - _Requirements: 4.1, 4.3, 4.4, 4.5, 11.8_
  
  - [x] 13.4 Create CacheConfiguration class
    - Add @Configuration and @EnableCaching annotations
    - Configure Caffeine cache with maximumSize=1000 and expireAfterWrite=300s
    - Register "postgres-bookings" cache name
    - _Requirements: 9.7, 16.7_

- [x] 14. Update application.yml configuration
  - Add eventstore.subscription configuration (enabled, name, polling-interval, polling-initial-delay)
  - Add eventstore.snapshot.booking-order configuration (enabled, frequency)
  - Add spring.datasource.eventstore configuration with environment variables
  - Add spring.datasource.postgresql configuration with environment variables
  - Configure HikariCP settings for both datasources (maximum-pool-size, minimum-idle, connection-timeout, idle-timeout, max-lifetime)
  - Add spring.jpa configuration (dialect, format_sql, batch_size, order_inserts, order_updates, show-sql, ddl-auto, open-in-view)
  - Add spring.flyway configuration (enabled, locations, baseline-on-migrate, baseline-version, validate-on-migrate)
  - Add spring.cache configuration (type: caffeine, spec, cache-names)
  - Add logging configuration for query handler and Hibernate SQL
  - _Requirements: 1.2, 1.3, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 16.5, 16.6, 16.7, 20.1, 20.2, 20.3, 20.4, 20.5, 20.6, 20.7_

- [x] 15. Checkpoint - Verify basic functionality
  - Ensure all tests pass
  - Verify Flyway migrations execute successfully
  - Verify dual datasource configuration works
  - Verify entities can be persisted and queried
  - Ask the user if questions arise

- [ ]* 16. Write property test for Event Subscription Idempotency
  - **Property 1: Event Subscription Idempotency**
  - **Validates: Requirements 1.4, 18.7**
  - Generate arbitrary sequence of events for a booking aggregate
  - Process events once and capture final projection state
  - Process same events again and capture final projection state
  - Assert both states are identical (same booking data, paxes, products)
  - Run with minimum 100 iterations

- [ ]* 17. Write property test for Complete Event Retrieval
  - **Property 2: Complete Event Retrieval**
  - **Validates: Requirements 1.5**
  - Generate arbitrary aggregate ID and sequence of events
  - Store events in Event Store
  - Retrieve events for aggregate ID
  - Assert all events for that aggregate are retrieved
  - Assert no events from other aggregates are included
  - Run with minimum 100 iterations

- [ ]* 18. Write property test for Projection Handler Invocation
  - **Property 3: Projection Handler Invocation**
  - **Validates: Requirements 2.4, 2.5**
  - Generate arbitrary booking aggregate
  - Create mock projection handlers (some matching aggregate type, some not)
  - Invoke event handler with aggregate
  - Assert all handlers matching aggregate type were invoked exactly once
  - Assert handlers not matching aggregate type were not invoked
  - Run with minimum 100 iterations

- [ ]* 19. Write property test for Cascade Delete and Referential Integrity
  - **Property 4: Cascade Delete and Referential Integrity**
  - **Validates: Requirements 3.4, 3.5, 5.4, 5.5, 18.5, 18.6**
  - Generate arbitrary booking with N paxes and M products
  - Save booking entity to database
  - Delete booking entity
  - Assert all N paxes are deleted from database
  - Assert all M products are deleted from database
  - Test orphan removal by updating booking with fewer children
  - Run with minimum 100 iterations

- [ ]* 20. Write property test for Unique Booking Reference
  - **Property 5: Unique Booking Reference**
  - **Validates: Requirements 3.8**
  - Generate two distinct bookings with different IDs but same booking reference
  - Save first booking successfully
  - Attempt to save second booking with duplicate reference
  - Assert DataIntegrityViolationException is thrown
  - Run with minimum 100 iterations

- [ ]* 21. Write property test for Automatic Timestamp Management
  - **Property 6: Automatic Timestamp Management**
  - **Validates: Requirements 5.9**
  - Generate arbitrary booking entity
  - Save entity and capture created_at timestamp
  - Assert created_at is set automatically and not null
  - Update entity and capture updated_at timestamp
  - Assert updated_at is greater than created_at
  - Run with minimum 100 iterations

- [ ]* 22. Write property test for Mapping Completeness
  - **Property 7: Mapping Completeness**
  - **Validates: Requirements 6.2, 6.3, 6.5, 6.6**
  - Generate arbitrary booking aggregate with N passengers and M products
  - Convert aggregate to entity using mapper
  - Assert entity contains exactly N passenger entities
  - Assert entity contains exactly M product entities
  - Assert all passenger fields are preserved
  - Assert all product metadata fields are preserved
  - Run with minimum 100 iterations

- [ ]* 23. Write property test for Product Details Serialization Round-Trip
  - **Property 8: Product Details Serialization Round-Trip**
  - **Validates: Requirements 6.4, 14.1, 14.2, 14.3, 14.4, 14.5, 14.7**
  - Generate arbitrary transfer product with all fields
  - Serialize to JSONB using mapper
  - Deserialize from JSONB back to transfer object
  - Assert all fields match original (origin, destination, trips, vehicle, price)
  - Test with null inbound_trip for one-way transfers
  - Run with minimum 100 iterations

- [ ]* 24. Write property test for Aggregate to Entity Conversion
  - **Property 9: Aggregate to Entity Conversion**
  - **Validates: Requirements 7.2, 7.3**
  - Generate arbitrary booking aggregate
  - Convert to PostgreSQL entity using mapper
  - Save entity using projection service
  - Retrieve entity by booking ID
  - Assert retrieved entity has same booking ID and all core fields preserved
  - Run with minimum 100 iterations

- [ ]* 25. Write property test for Repository Query Correctness
  - **Property 10: Repository Query Correctness**
  - **Validates: Requirements 8.2, 8.3, 8.4, 8.5, 8.6**
  - Generate arbitrary booking entity and save to database
  - Query by booking reference and assert correct booking returned
  - Query by status and assert booking is in results
  - Query by lead passenger ID and assert booking is in results
  - Query by passenger email and assert booking is in results
  - Query by ID with details and assert all paxes and products eagerly fetched
  - Run with minimum 100 iterations

- [ ]* 26. Write property test for Service Retrieval Completeness
  - **Property 11: Service Retrieval Completeness**
  - **Validates: Requirements 9.2, 9.3, 9.4**
  - Generate arbitrary booking with passengers and products
  - Save to PostgreSQL using projection service
  - Retrieve by ID through service layer
  - Assert complete booking with all passengers and products returned
  - Retrieve by reference through service layer
  - Assert same complete booking returned
  - Run with minimum 100 iterations

- [ ]* 27. Write property test for Snapshot Optimization Efficiency
  - **Property 12: Snapshot Optimization Efficiency**
  - **Validates: Requirements 12.3, 12.7**
  - Generate aggregate with snapshot at version V and additional events after V
  - Mock Event Store to track which events are loaded
  - Rebuild aggregate using snapshot optimization
  - Assert only events with version > V are loaded
  - Assert aggregate final state is correct
  - Run with minimum 100 iterations

- [ ]* 28. Write property test for Snapshot Fallback Completeness
  - **Property 13: Snapshot Fallback Completeness**
  - **Validates: Requirements 12.4, 12.5**
  - Generate aggregate without snapshot
  - Generate sequence of events from beginning
  - Rebuild aggregate without snapshot
  - Assert all events from beginning are loaded
  - Assert aggregate final state is correct
  - Run with minimum 100 iterations

- [ ]* 29. Write property test for Offset Management on Success
  - **Property 14: Offset Management on Success**
  - **Validates: Requirements 13.6**
  - Generate batch of events
  - Mock all projection handlers to succeed
  - Process batch through event handler
  - Assert subscription offset is updated to last processed event
  - Run with minimum 100 iterations

- [ ]* 30. Write property test for Offset Preservation on Failure
  - **Property 15: Offset Preservation on Failure**
  - **Validates: Requirements 13.2**
  - Generate batch of events
  - Mock one projection handler to fail
  - Process batch through event handler
  - Assert exception is propagated
  - Assert subscription offset is not updated
  - Run with minimum 100 iterations

- [ ]* 31. Write property test for Batch Deduplication
  - **Property 16: Batch Deduplication**
  - **Validates: Requirements 17.1**
  - Generate batch with multiple events for same aggregate ID
  - Process batch through event handler
  - Assert aggregate is rebuilt only once using latest version
  - Assert only unique aggregate IDs are processed
  - Run with minimum 100 iterations

- [ ]* 32. Write property test for Aggregate Processing Isolation
  - **Property 17: Aggregate Processing Isolation**
  - **Validates: Requirements 17.3, 17.4**
  - Generate batch with events for two distinct aggregates
  - Process batch in parallel
  - Assert processing one aggregate does not affect state of other aggregate
  - Assert both aggregates are processed independently
  - Run with minimum 100 iterations

- [ ]* 33. Write property test for Upsert Behavior
  - **Property 18: Upsert Behavior**
  - **Validates: Requirements 18.2**
  - Generate arbitrary booking entity and save to database
  - Modify booking entity fields
  - Save modified entity with same ID
  - Assert existing record is updated, not duplicated
  - Query database and assert only one record exists with that ID
  - Run with minimum 100 iterations

- [ ]* 34. Write property test for Projection Consistency
  - **Property 19: Projection Consistency**
  - **Validates: Requirements 15.3, 15.4, 15.5, 15.6**
  - Generate arbitrary booking aggregate
  - Process through both MongoDB and PostgreSQL projection handlers
  - Retrieve booking from both projections
  - Assert booking ID, reference, status, lead passenger ID match
  - Assert number of passengers matches
  - Assert number of products matches
  - Run with minimum 100 iterations

- [ ]* 35. Write unit test for exception propagation
  - **Example 1: Exception Propagation**
  - **Validates: Requirements 2.6, 7.6, 13.1**
  - Create booking aggregate
  - Mock projection service to throw exception
  - Invoke projection handler
  - Assert exception is propagated to caller
  - Verify exception is logged at error level

- [ ]* 36. Write unit test for not found exception
  - **Example 2: Not Found Exception**
  - **Validates: Requirements 9.5**
  - Call service.retrieveById with non-existent booking ID
  - Assert BookingNotFoundException is thrown
  - Verify exception message contains booking ID

- [ ]* 37. Write unit test for optimistic locking conflict
  - **Example 3: Optimistic Locking Conflict**
  - **Validates: Requirements 5.8, 18.3, 18.4**
  - Save booking entity to database
  - Load same entity in two separate transactions
  - Modify and save first entity
  - Modify and attempt to save second entity
  - Assert OptimisticLockException is thrown on second save

- [ ]* 38. Write unit test for null handling in optional fields
  - **Example 4: Null Handling in Optional Fields**
  - **Validates: Requirements 6.7, 14.6**
  - Create transfer product with null inbound_trip (one-way transfer)
  - Convert to entity using mapper
  - Assert no NullPointerException is thrown
  - Assert productDetails JSONB contains null for inbound_trip field

- [ ]* 39. Write unit test for aggregate processing failure isolation
  - **Example 5: Aggregate Processing Failure Isolation**
  - **Validates: Requirements 17.5**
  - Create batch with multiple aggregates
  - Mock projection handler to fail for one specific aggregate
  - Process batch
  - Assert failure for one aggregate does not prevent retry for other aggregates
  - Verify other aggregates can be processed successfully in subsequent retry

- [ ]* 40. Write integration tests for end-to-end event processing
  - Test event processing from Event Store to both projections
  - Test REST API endpoints return correct HTTP status codes (200, 404)
  - Test database migrations execute successfully on startup
  - Test dual datasource configuration works correctly
  - Test connection pooling handles concurrent requests
  - Test caching behavior (cache hit, cache miss, cache eviction)

- [ ] 41. Final checkpoint - Verify complete implementation
  - Ensure all tests pass (unit, property, integration)
  - Verify all correctness properties are validated
  - Verify projection consistency between MongoDB and PostgreSQL
  - Verify API endpoints work correctly
  - Verify error handling and retry logic
  - Ask the user if questions arise

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties with minimum 100 iterations
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end system behavior
- The implementation uses Java with Spring Boot, JPA, and Flyway
- Snapshot optimization is critical for performance and should be verified in testing
- All projection handlers must propagate exceptions to ensure retry on failure
