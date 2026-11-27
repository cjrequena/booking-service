package com.cjrequena.sample.query.handler.domain.exception;

public class DeadLetterProjectionNotFoundException extends DomainRuntimeException {
  public DeadLetterProjectionNotFoundException(String message) {
    super(message);
  }
}
