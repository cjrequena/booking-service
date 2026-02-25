package com.cjrequena.sample.es.core.service;


import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.exception.OptimisticConcurrencyException;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.AbstractEventEntity;
import com.cjrequena.sample.es.core.persistence.entity.AggregateSnapshotEntity;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import com.cjrequena.sample.es.core.persistence.entity.EventSubscriptionEntity;
import com.cjrequena.sample.es.core.persistence.repository.AggregateRepository;
import com.cjrequena.sample.es.core.persistence.repository.AggregateSnapshotRepository;
import com.cjrequena.sample.es.core.persistence.repository.EventRepository;
import com.cjrequena.sample.es.core.persistence.repository.EventSubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing event store operations including aggregate persistence,
 * event appending, snapshot management, and event subscription handling.
 * <p>
 * This service provides the core functionality for event sourcing:
 * <ul>
 *   <li>Saving aggregates with optimistic concurrency control</li>
 *   <li>Appending events to the event store</li>
 *   <li>Creating and retrieving aggregate snapshots</li>
 *   <li>Managing event subscriptions for event polling</li>
 *   <li>Querying events by various criteria</li>
 * </ul>
 * </p>
 * <p>
 * All write operations are transactional and use optimistic locking to prevent
 * concurrent modification conflicts.
 * </p>
 */
@Service
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class EventStoreService {

  private final AggregateRepository aggregateRepository;
  private final AggregateSnapshotRepository aggregateSnapshotRepository;
  private final EventRepository eventRepository;
  private final EventSubscriptionRepository eventSubscriptionRepository;
  private final EventStoreConfigurationProperties eventStoreConfigurationProperties;
  private final ObjectMapper objectMapper;

  /**
   * Saves an aggregate to the event store with optimistic concurrency control.
   * <p>
   * This method performs the following operations:
   * <ol>
   *   <li>Creates the aggregate entry if it doesn't exist</li>
   *   <li>Flushes to ensure aggregate is persisted before events</li>
   *   <li>Verifies and updates the aggregate version (optimistic locking)</li>
   *   <li>Appends all unconfirmed events to the event store</li>
   *   <li>Creates a snapshot if the snapshot interval is reached</li>
   * </ol>
   * </p>
   *
   * @param aggregate the aggregate to save; must not be {@code null}
   * @throws OptimisticConcurrencyException if the aggregate version has been modified by another transaction
   * @throws JsonProcessingException if snapshot serialization fails
   */
  public void saveAggregate(@NotNull Aggregate aggregate) throws OptimisticConcurrencyException, JsonProcessingException {
    String aggregateType = aggregate.getAggregateType();
    UUID aggregateId = aggregate.getAggregateId();

    // Create new aggregate if it does not exist
    this.aggregateRepository.createAggregateIfAbsent(aggregateId, aggregateType);

    long expectedAggregateVersion = aggregate.getReproducedAggregateVersion();
    long newAggregateVersion = aggregate.getAggregateVersion();

    // Verify and update aggregate version with optimistic locking
    Optional<Integer> isVersionUpdated = aggregateRepository.verifyAndUpdateAggregateVersionIfMatch(
        aggregateId, expectedAggregateVersion, newAggregateVersion
    );
    
    if (isVersionUpdated.isEmpty()) {
      String errorMessage = String.format(
        "Optimistic concurrency conflict detected for aggregate '%s' with ID '%s'. " +
        "Expected version %d but current version is different.",
        aggregateType, aggregateId, expectedAggregateVersion
      );
      log.warn(errorMessage);
      throw new OptimisticConcurrencyException(errorMessage);
    }

    // Append new events
    List<Event> unconfirmedEventsPool = aggregate.getUnconfirmedEventsPool();
    for (Event event : unconfirmedEventsPool) {
      log.info("Appending {} event: {}", aggregateType, event);
      AbstractEventEntity eventEntity = event.mapToEventEntity();
      eventRepository.save(eventEntity);
    }

    // Create snapshot if configured and interval is reached
    createSnapshotIfNeeded(aggregate);
  }

  /**
   * Creates a snapshot of the aggregate if the snapshot interval is reached.
   * <p>
   * Snapshots are created when:
   * <ul>
   *   <li>Snapshots are enabled for the aggregate type</li>
   *   <li>The aggregate version is a multiple of the snapshot interval</li>
   * </ul>
   * </p>
   *
   * @param aggregate the aggregate to snapshot
   * @throws JsonProcessingException if serialization fails
   */
  private void createSnapshotIfNeeded(Aggregate aggregate) throws JsonProcessingException {
    String aggregateType = aggregate.getAggregateType();
    EventStoreConfigurationProperties.SnapshotProperties snapshotProperties = eventStoreConfigurationProperties.getSnapshot(aggregateType);
    
    if (!snapshotProperties.enabled()) {
      return;
    }
    
    boolean shouldCreateSnapshot = aggregate.getAggregateVersion() % snapshotProperties.interval() == 0;
    
    if (shouldCreateSnapshot) {
      log.info("Creating snapshot for {} with aggregate ID '{}' at version {}", aggregateType, aggregate.getAggregateId(), aggregate.getAggregateVersion());
      
      AggregateSnapshotEntity aggregateSnapshotEntity = AggregateSnapshotEntity.builder()
        .aggregateId(aggregate.getAggregateId())
        .aggregateVersion(aggregate.getAggregateVersion())
        .aggregateType(aggregateType)
        .data(this.objectMapper.writeValueAsString(aggregate))
        .build();
      
      this.aggregateSnapshotRepository.save(aggregateSnapshotEntity);
    }
  }

  /**
   * Retrieves the most recent snapshot for an aggregate.
   * <p>
   * If an aggregate version is specified, returns the latest snapshot at or before that version.
   * Otherwise, returns the most recent snapshot available.
   * </p>
   *
   * @param aggregateClass the class type of the aggregate; must not be {@code null}
   * @param aggregateId the unique identifier of the aggregate; must not be {@code null}
   * @param aggregateVersion the maximum version to consider; may be {@code null} for latest
   * @param <T> the type of the aggregate
   * @return an Optional containing the aggregate snapshot if found, empty otherwise
   */
  @Transactional(readOnly = true)
  public <T extends Aggregate> Optional<T> retrieveAggregateSnapshot(
      @NotNull Class<T> aggregateClass, 
      @NotNull UUID aggregateId, 
      @Nullable Long aggregateVersion) {
    
    log.info("Retrieving aggregate snapshot for {} with ID '{}'", aggregateClass.getSimpleName(), aggregateId);

    return aggregateSnapshotRepository
      .retrieveAggregateSnapshot(aggregateId, aggregateVersion)
      .map(snapshot -> fromSnapshotToAggregate(snapshot, aggregateClass));
  }

  /**
   * Retrieves events for a specific aggregate within an optional version range.
   * <p>
   * This method is used to reconstitute aggregates from their event history.
   * </p>
   *
   * @param aggregateId the unique identifier of the aggregate; must not be {@code null}
   * @param fromAggregateVersion the lower bound version (exclusive); may be {@code null}
   * @param toAggregateVersion the upper bound version (inclusive); may be {@code null}
   * @return list of events for the aggregate, ordered by version
   * @throws IllegalArgumentException if aggregateId is null
   */
  @Transactional(readOnly = true)
  public List<EventEntity> retrieveEventsByAggregateId(
      @NotNull UUID aggregateId, 
      @Nullable Long fromAggregateVersion, 
      @Nullable Long toAggregateVersion) {
    
    if (log.isInfoEnabled()) {
      if (fromAggregateVersion != null && toAggregateVersion != null) {
        log.info("Retrieving events for aggregate '{}' from version {} to {}", 
            aggregateId, fromAggregateVersion, toAggregateVersion);
      } else if (fromAggregateVersion != null) {
        log.info("Retrieving events for aggregate '{}' from version {}", aggregateId, fromAggregateVersion);
      } else {
        log.info("Retrieving all events for aggregate '{}'", aggregateId);
      }
    }
    
    if (aggregateId == null) {
      throw new IllegalArgumentException("aggregateId cannot be null");
    }
    
    return eventRepository.retrieveEventsByAggregateId(aggregateId, fromAggregateVersion, toAggregateVersion);
  }

  /**
   * Verifies if an aggregate exists with the specified ID and type.
   *
   * @param aggregateId the unique identifier of the aggregate; must not be {@code null}
   * @param aggregateType the type of the aggregate; must not be {@code null}
   * @return {@code true} if the aggregate exists, {@code false} otherwise
   */
  @Transactional(readOnly = true)
  public boolean verifyIfAggregateExist(@NotNull UUID aggregateId, @NotNull String aggregateType) {
    return this.aggregateRepository.verifyIfAggregateExist(aggregateId, aggregateType);
  }

  /**
   * Registers a new event subscription if it doesn't already exist.
   * <p>
   * New subscriptions are initialized with offset 0, meaning they will start
   * processing events from the beginning of the event stream.
   * </p>
   *
   * @param subscriptionName the unique name of the subscription; must not be {@code null}
   */
  public void registerNewSubscriptionIfAbsent(@NotNull String subscriptionName) {
    this.eventSubscriptionRepository.registerNewSubscriptionIfAbsent(subscriptionName);
  }

  /**
   * Retrieves a subscription and locks it for update.
   * <p>
   * Uses PostgreSQL's FOR UPDATE SKIP LOCKED to implement competing consumers pattern.
   * If the subscription is already locked by another consumer, returns empty.
   * </p>
   *
   * @param subscriptionName the unique name of the subscription; must not be {@code null}
   * @return an Optional containing the subscription if the lock was acquired, empty if already locked
   */
  @Transactional(readOnly = true)
  public Optional<EventSubscriptionEntity> retrieveEventSubscriptionAndLockSubscriptionOffset(
      @NotNull String subscriptionName) {
    return this.eventSubscriptionRepository.retrieveEventSubscriptionAndLockSubscriptionOffset(subscriptionName);
  }

  /**
   * Retrieves events of a specific aggregate type after a given offset position.
   * <p>
   * This method is used for event polling by subscriptions. It returns events that
   * are visible in the current transaction snapshot.
   * </p>
   *
   * @param aggregateType the type of aggregate; must not be {@code null}
   * @param offsetTxId the transaction ID after which to retrieve events; must not be {@code null}
   * @param offsetId the offset ID after which to retrieve events; must not be {@code null}
   * @return list of events after the specified offset, ordered by commit sequence
   */
  @Transactional(readOnly = true)
  public List<EventEntity> retrieveEventsByAggregateTypeAfterOffsetTxIdAndOffsetId(
      @NotNull String aggregateType, 
      @NotNull Long offsetTxId, 
      @NotNull Long offsetId) {
    return this.eventRepository.retrieveEventsByAggregateTypeAfterOffsetTxIdAndOffsetId(
        aggregateType, offsetTxId, offsetId);
  }

  /**
   * Retrieves the latest event for each specified aggregate ID.
   *
   * @param aggregateType the type of aggregate; must not be {@code null}
   * @param aggregateIds list of aggregate IDs; must not be {@code null}
   * @return list of the most recent events for each aggregate
   */
  @Transactional(readOnly = true)
  public List<EventEntity> retrieveLatestEventsByAggregateTypeAndAggregateIds(
      @NotNull String aggregateType, 
      @NotNull List<UUID> aggregateIds) {
    return this.eventRepository.retrieveLatestEventsByAggregateTypeAndAggregateIds(aggregateType, aggregateIds);
  }

  /**
   * Retrieves the latest event for a specific aggregate.
   *
   * @param aggregateType the type of aggregate; must not be {@code null}
   * @param aggregateId the unique identifier of the aggregate; must not be {@code null}
   * @return an Optional containing the latest event if found, empty otherwise
   */
  @Transactional(readOnly = true)
  public Optional<EventEntity> retrieveLatestEventByAggregateTypeAndAggregateId(
      @NotNull String aggregateType, 
      @NotNull UUID aggregateId) {
    return this.eventRepository.retrieveLatestEventByAggregateTypeAndAggregateId(aggregateType, aggregateId);
  }

  /**
   * Retrieves the latest event for each aggregate of a specific type.
   *
   * @param aggregateType the type of aggregate; must not be {@code null}
   * @return list of the most recent events for each aggregate of the specified type
   */
  @Transactional(readOnly = true)
  public List<EventEntity> retrieveLatestEventsByAggregateType(@NotNull String aggregateType) {
    return this.eventRepository.retrieveLatestEventsByAggregateType(aggregateType);
  }

  /**
   * Updates the subscription cursor to the specified offset position.
   * <p>
   * This should be called after successfully processing a batch of events.
   * </p>
   *
   * @param subscriptionName the unique name of the subscription; must not be {@code null}
   * @param offsetTxId the transaction ID of the last processed event; must not be {@code null}
   * @param offsetId the offset ID of the last processed event; must not be {@code null}
   * @return {@code true} if the update was successful, {@code false} if subscription not found
   */
  public boolean updateEventSubscription(
      @NotNull String subscriptionName, 
      @NotNull Long offsetTxId, 
      @NotNull Long offsetId) {
    final int rowsUpdated = this.eventSubscriptionRepository.updateEventSubscription(
        subscriptionName, offsetTxId, offsetId);
    return rowsUpdated > 0;
  }

  /**
   * Converts a snapshot entity to an aggregate domain object.
   *
   * @param aggregateSnapshotEntity the snapshot entity to convert
   * @param aggregateClass the target aggregate class
   * @param <T> the type of the aggregate
   * @return the reconstituted aggregate
   * @throws RuntimeException if deserialization fails
   */
  private <T extends Aggregate> T fromSnapshotToAggregate(
      AggregateSnapshotEntity aggregateSnapshotEntity, 
      Class<T> aggregateClass) {
    try {
      String json = aggregateSnapshotEntity.getData();
      final T aggregate = objectMapper.readValue(json, aggregateClass);
      aggregate.setReproducedAggregateVersion(aggregate.getAggregateVersion());
      return aggregate;
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to deserialize aggregate snapshot", e);
    }
  }

}
