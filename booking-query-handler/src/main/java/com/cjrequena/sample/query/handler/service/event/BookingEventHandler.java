package com.cjrequena.sample.query.handler.service.event;

import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import com.cjrequena.sample.es.core.service.EventStoreService;
import com.cjrequena.sample.query.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.query.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.query.handler.service.projection.ProjectionHandler;
import jakarta.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
@Log4j2
public class BookingEventHandler extends EventHandler {

  private final List<ProjectionHandler> projectionHandlers;
  @Value("${projections.handlers.enabled}")
  private boolean projectionsHandlersEnabled;

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

    final List<Event> events = this.eventMapper.toEventList(eventEntityList);

    if (projectionsHandlersEnabled) {
      // Save or Update the projection database
      events.parallelStream()
        .map(Event::getAggregateId)
        .distinct()
        .forEach(aggregateId -> {
          final Aggregate aggregate = retrieveOrInstantiateAggregate(aggregateId);
          projectionHandlers.stream()
            .filter(handler -> handler.getAggregateType().getType().equals(aggregate.getAggregateType()))
            .forEach(handler -> handler.handle(aggregate));
        });
    }

  }

  @Nonnull
  @Override
  public AggregateType getAggregateType() {
    return AggregateType.BOOKING_ORDER;
  }
}
