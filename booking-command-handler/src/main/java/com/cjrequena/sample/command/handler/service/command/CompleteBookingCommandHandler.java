package com.cjrequena.sample.command.handler.service.command;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.command.CompleteBookingCommand;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.exception.AggregateNotFoundException;
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
public class CompleteBookingCommandHandler extends CommandHandler<CompleteBookingCommand> {

  @Autowired
  public CompleteBookingCommandHandler(
    EventStoreService eventStoreService,
    EventMapper eventMapper,
    EventStoreConfigurationProperties eventStoreConfigurationProperties
  ) {
    super(eventStoreService, eventMapper, eventStoreConfigurationProperties);
  }

  @Override
  public Aggregate handle(@Nonnull Command command) {
    log.trace("Handling command of type {} for aggregate {}", command.getClass().getSimpleName(), command.getAggregateType());

    if (!(command instanceof CompleteBookingCommand)) {
      throw new IllegalArgumentException("Expected command of type CompleteBookingCommand but received " + command.getClass().getSimpleName());
    }

    if (!this.eventStoreService.verifyIfAggregateExist(command.getAggregateId(), command.getAggregateType())) {
      String errorMessage = String.format(
        "The aggregate '%s' with ID '%s' does not exist'.", command.getAggregateType(), command.getAggregateId()
      );
      throw new AggregateNotFoundException(errorMessage);
    }

    Aggregate aggregate = retrieveOrInstantiateAggregate(command.getAggregateId());

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
  public Class<CompleteBookingCommand> getCommandType() {
    return CompleteBookingCommand.class;
  }

  @Nonnull
  @Override
  public AggregateType getAggregateType() {
    return AggregateType.BOOKING_ORDER;
  }
}
