package com.cjrequena.sample.command.handler.domain.exception;


public class InvalidArgumentException extends DomainRuntimeException {
  public InvalidArgumentException(String message) {
    super(message);
  }
}
