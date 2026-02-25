package com.cjrequena.sample.command.handler.domain.exception;


public class CommandHandlerNotFoundException extends DomainRuntimeException {
  public CommandHandlerNotFoundException(String message) {
    super(message);
  }
}
