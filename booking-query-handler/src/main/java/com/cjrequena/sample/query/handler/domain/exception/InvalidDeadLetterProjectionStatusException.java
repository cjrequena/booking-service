package com.cjrequena.sample.query.handler.domain.exception;

/** The requested transition is not allowed from the dead-letter's current status. */
public class InvalidDeadLetterProjectionStatusException extends DomainRuntimeException {
  public InvalidDeadLetterProjectionStatusException(String message) {
    super(message);
  }
}
