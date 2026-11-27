package com.cjrequena.sample.command.handler.service.event;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import com.cjrequena.sample.es.core.service.AggregateLoader;
import com.cjrequena.sample.es.core.service.EventStoreService;
import jakarta.annotation.Nonnull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Transactional
public abstract class EventHandler {

  protected final EventStoreService eventStoreService;
  protected final EventMapper eventMapper;
  protected final EventStoreConfigurationProperties eventStoreConfigurationProperties;
  private final AggregateLoader aggregateLoader;

  protected EventHandler(EventStoreService eventStoreService,
                         EventMapper eventMapper,
                         EventStoreConfigurationProperties eventStoreConfigurationProperties) {
    this.eventStoreService = eventStoreService;
    this.eventMapper = eventMapper;
    this.eventStoreConfigurationProperties = eventStoreConfigurationProperties;
    this.aggregateLoader = new AggregateLoader(eventStoreService, eventMapper::toEventList, eventStoreConfigurationProperties);
  }

  public abstract void handle(List<EventEntity> eventEntityList);

  @Nonnull
  public abstract AggregateType getAggregateType();

  protected Aggregate retrieveOrInstantiateAggregate(UUID aggregateId) {
    return aggregateLoader.retrieveOrInstantiateAggregate(getAggregateType().getType(), getAggregateType().getClazz(), aggregateId);
  }
}
