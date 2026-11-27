package com.cjrequena.sample.command.handler.service.command;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.exception.OptimisticConcurrencyException;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import com.cjrequena.sample.es.core.service.AggregateLoader;
import com.cjrequena.sample.es.core.service.EventStoreService;
import jakarta.annotation.Nonnull;

import java.util.UUID;

// Intentionally NOT @Transactional at the class level. Reconstitution reads and the persist run in
// EventStoreService's own transactions; wrapping the whole handle() in one transaction would pin an
// event store connection for the full command, including any downstream calls a handler makes. A
// stale reconstituted version is still caught at save time by the optimistic version check
// (EventStoreService#saveAggregate), so reconstitute and save need not share a transaction.
public abstract class CommandHandler<T extends Command> {

  protected final EventStoreService eventStoreService;
  protected final EventMapper eventMapper;
  protected final EventStoreConfigurationProperties eventStoreConfigurationProperties;
  private final AggregateLoader aggregateLoader;

  protected CommandHandler(EventStoreService eventStoreService,
                           EventMapper eventMapper,
                           EventStoreConfigurationProperties eventStoreConfigurationProperties) {
    this.eventStoreService = eventStoreService;
    this.eventMapper = eventMapper;
    this.eventStoreConfigurationProperties = eventStoreConfigurationProperties;
    this.aggregateLoader = new AggregateLoader(eventStoreService, eventMapper::toEventList, eventStoreConfigurationProperties);
  }

  public abstract Aggregate handle(@Nonnull Command command) throws OptimisticConcurrencyException;

  @Nonnull
  public abstract Class<T> getCommandType();

  @Nonnull
  public abstract AggregateType getAggregateType();

  protected Aggregate retrieveOrInstantiateAggregate(UUID aggregateId) {
    return aggregateLoader.retrieveOrInstantiateAggregate(getAggregateType().getType(), getAggregateType().getClazz(), aggregateId);
  }
}
