package com.cjrequena.sample.query.handler.domain.exception;


public class InvalidArgumentException extends DomainRuntimeException {
  public InvalidArgumentException(String message) {
    super(message);
  }
}
