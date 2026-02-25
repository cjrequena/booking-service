package com.cjrequena.sample.command.handler.domain.exception;

import lombok.ToString;

@ToString
public abstract class DomainException extends Exception {
  public DomainException(Throwable ex) {
    super(ex);
  }

  public DomainException(String message) {
    super(message);
  }

  public DomainException(String message, Throwable ex) {
    super(message, ex);
  }

}
