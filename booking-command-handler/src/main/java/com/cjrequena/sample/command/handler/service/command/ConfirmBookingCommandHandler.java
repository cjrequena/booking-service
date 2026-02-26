package com.cjrequena.sample.command.handler.service.command;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.command.ConfirmBookingCommand;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.exception.OptimisticConcurrencyException;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import com.cjrequena.sample.es.core.service.EventStoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@Transactional
public class ConfirmBookingCommandHandler extends CommandHandler<ConfirmBookingCommand> {

  @Autowired
  public ConfirmBookingCommandHandler(
    EventStoreService eventStoreService,
    EventMapper eventMapper,
    EventStoreConfigurationProperties eventStoreConfigurationProperties
  ) {
    super(eventStoreService, eventMapper, eventStoreConfigurationProperties);
  }

  @Override
  public Aggregate handle(@Nonnull Command command) {
    log.trace("Handling command of type {} for aggregate {}", command.getClass().getSimpleName(), command.getAggregateType());

    if (!(command instanceof ConfirmBookingCommand)) {
      throw new IllegalArgumentException("Expected command of type ConfirmBookingCommand but received " + command.getClass().getSimpleName());
    }

    // Get the current aggregate
    Aggregate aggregate = retrieveOrInstantiateAggregate(command.getAggregateId());

    // Apply command and persist aggregate state
    try {
      aggregate.applyCommand(command);
      eventStoreService.saveAggregate(aggregate);
      aggregate.markUnconfirmedEventsAsConfirmed();
    } catch (OptimisticConcurrencyException ex) {
      throw new com.cjrequena.sample.command.handler.domain.exception.OptimisticConcurrencyException(ex.getMessage(), ex);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    log.info("Successfully handled command {} for aggregate with ID {}", command.getClass().getSimpleName(), command.getAggregateId());

    return aggregate;
  }

  @Nonnull
  @Override
  public Class<ConfirmBookingCommand> getCommandType() {
    return ConfirmBookingCommand.class;
  }

  @Nonnull
  @Override
  public AggregateType getAggregateType() {
    return AggregateType.BOOKING_ORDER;
  }
}
