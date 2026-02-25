package com.cjrequena.sample.command.handler.service.command;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.exception.OptimisticConcurrencyException;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.service.AggregateFactory;
import com.cjrequena.sample.es.core.service.EventStoreService;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public abstract class CommandHandler<T extends Command> {

  protected final EventStoreService eventStoreService;
  protected final EventMapper eventMapper;
  protected final EventStoreConfigurationProperties eventStoreConfigurationProperties;

  public abstract Aggregate handle(@Nonnull Command command) throws OptimisticConcurrencyException;

  @Nonnull
  public abstract Class<T> getCommandType();

  @Nonnull
  public abstract AggregateType getAggregateType();

  protected Aggregate retrieveOrInstantiateAggregate(UUID aggregateId) {
    final EventStoreConfigurationProperties.SnapshotProperties snapshotConfiguration = eventStoreConfigurationProperties.getSnapshot(
      getAggregateType().getType());
    if (snapshotConfiguration.enabled()) {
      return retrieveAggregateFromSnapshot(aggregateId)
        .orElseGet(() -> createAndReproduceAggregate(aggregateId));
    } else {
      return createAndReproduceAggregate(aggregateId);
    }
  }

  protected Optional<Aggregate> retrieveAggregateFromSnapshot(UUID aggregateId) {
    final Optional<? extends Aggregate> optionalAggregate = eventStoreService.retrieveAggregateSnapshot(getAggregateType().getClazz(), aggregateId, null);
    return optionalAggregate.map(aggregate -> {
      List<Event> events = retrieveEvents(aggregateId, aggregate.getAggregateVersion());
      aggregate.reproduceFromEvents(events);
      return aggregate;
    });
  }

  protected Aggregate createAndReproduceAggregate(UUID aggregateId) {
    log.info("Snapshot not found for Aggregate ID: {}. Reconstituting from events.", aggregateId);
    Aggregate aggregate = AggregateFactory.newInstance(getAggregateType().getClazz(), aggregateId);
    List<Event> events = retrieveEvents(aggregateId, null);
    aggregate.reproduceFromEvents(events);
    return aggregate;
  }

  protected List<Event> retrieveEvents(UUID aggregateId, Long fromVersion) {
    return eventMapper.toEventList(eventStoreService.retrieveEventsByAggregateId(aggregateId, fromVersion, null));
  }
}
