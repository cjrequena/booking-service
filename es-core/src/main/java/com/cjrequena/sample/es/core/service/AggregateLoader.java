package com.cjrequena.sample.es.core.service;

import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Reconstitutes an aggregate from its latest snapshot (when snapshots are enabled for its type) plus
 * the events after it, or from its full event stream.
 *
 * <p>The one load path shared by command handlers and event handlers. It knows nothing about a
 * service's aggregate or event types: the caller names the aggregate type and class, and supplies
 * the function that turns stored {@link EventEntity} rows into its domain {@link Event}s.</p>
 *
 * <p>Not transactional on its own: each {@link EventStoreService} read runs in that service's
 * transaction. A version that goes stale between load and save is caught by the optimistic check
 * in {@link EventStoreService#saveAggregate}.</p>
 */
@Log4j2
@RequiredArgsConstructor
public class AggregateLoader {

  private final EventStoreService eventStoreService;
  private final Function<List<EventEntity>, List<Event>> eventMapper;
  private final EventStoreConfigurationProperties eventStoreConfigurationProperties;

  /**
   * @param aggregateType  logical aggregate type, as configured under {@code eventstore.snapshot}
   * @param aggregateClass concrete aggregate class to instantiate
   * @param aggregateId    aggregate identifier
   * @return the aggregate with every stored event applied; a fresh instance when none exist yet
   */
  public Aggregate retrieveOrInstantiateAggregate(@NonNull String aggregateType,
                                                  @NonNull Class<? extends Aggregate> aggregateClass,
                                                  @NonNull UUID aggregateId) {
    if (eventStoreConfigurationProperties.getSnapshot(aggregateType).enabled()) {
      return retrieveAggregateFromSnapshot(aggregateClass, aggregateId)
        .orElseGet(() -> createAndReproduceAggregate(aggregateClass, aggregateId));
    }
    return createAndReproduceAggregate(aggregateClass, aggregateId);
  }

  private Optional<Aggregate> retrieveAggregateFromSnapshot(Class<? extends Aggregate> aggregateClass, UUID aggregateId) {
    return eventStoreService
      .retrieveAggregateSnapshot(aggregateClass, aggregateId, null)
      .map(aggregate -> {
        aggregate.reproduceFromEvents(retrieveEvents(aggregateId, aggregate.getAggregateVersion()));
        return aggregate;
      });
  }

  private Aggregate createAndReproduceAggregate(Class<? extends Aggregate> aggregateClass, UUID aggregateId) {
    log.debug("Snapshot not found for aggregate {}. Reconstituting from events.", aggregateId);
    Aggregate aggregate = AggregateFactory.newInstance(aggregateClass, aggregateId);
    aggregate.reproduceFromEvents(retrieveEvents(aggregateId, null));
    return aggregate;
  }

  private List<Event> retrieveEvents(UUID aggregateId, Long fromVersion) {
    return eventMapper.apply(eventStoreService.retrieveEventsByAggregateId(aggregateId, fromVersion, null));
  }
}
