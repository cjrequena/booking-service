package com.cjrequena.sample.command.handler.service.command;

import com.cjrequena.sample.command.handler.domain.exception.CommandHandlerNotFoundException;
import com.cjrequena.sample.command.handler.service.projection.ProjectionHandler;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CommandBusService {

  private final List<CommandHandler<? extends Command>> commandHandlers;
  private final List<ProjectionHandler> projectionHandlers;

  public void handle(Command command) {
    commandHandlers.stream()
      .filter(commandHandler -> commandHandler.getCommandType() == command.getClass())
      .findFirst()
      .ifPresentOrElse(commandHandler -> {
        log.info("Handling command {} with {}", command.getClass().getSimpleName(), commandHandler.getClass().getSimpleName());

        final Aggregate aggregate = commandHandler.handle(command);

        // Save or Update the projection database
        projectionHandlers.stream()
          .filter(handler -> handler.getAggregateType().getType().equals(aggregate.getAggregateType()))
          .forEach(handler -> handler.handle(aggregate));

      }, () -> {
        log.info("No specialized handle found with {}", command.getClass().getSimpleName());
        throw new CommandHandlerNotFoundException("No specialized handle found for command: " + command.getClass().getSimpleName());
      });
  }
}
