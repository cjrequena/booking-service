package com.cjrequena.sample.command.handler.service.event;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import com.cjrequena.sample.es.core.service.EventStoreService;
import jakarta.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Publishes every new booking event to the application's listeners (e.g. the Kafka emitter), in
 * event store order. Integration concerns stay out of this class: adding SNS, a webhook, etc. means
 * adding a listener, not editing the handler.
 */
@Component
@Transactional
@Log4j2
public class BookingEventHandler extends EventHandler {

  private final ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public BookingEventHandler(
    EventStoreService eventStoreService,
    EventMapper eventMapper,
    EventStoreConfigurationProperties eventStoreConfigurationProperties,
    ApplicationEventPublisher applicationEventPublisher
  ) {
    super(eventStoreService, eventMapper, eventStoreConfigurationProperties);
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void handle(List<EventEntity> eventEntityList) {
    final List<Event> events = this.eventMapper.toEventList(eventEntityList);
    for (Event event : events) {
      log.info(
        "Handling event {} for aggregate {} with ID '{}' and aggregate version {}",
        event.getEventType(), getAggregateType(), event.getAggregateId(), event.getAggregateVersion()
      );
      applicationEventPublisher.publishEvent(event);
    }
  }

  @Nonnull
  @Override
  public AggregateType getAggregateType() {
    return AggregateType.BOOKING_ORDER;
  }
}
