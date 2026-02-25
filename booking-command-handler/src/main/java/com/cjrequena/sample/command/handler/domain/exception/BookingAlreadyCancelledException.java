package com.cjrequena.sample.command.handler.domain.exception;

public class BookingAlreadyCancelledException extends DomainRuntimeException {
  public BookingAlreadyCancelledException(Throwable ex) {
    super(ex);
  }

  public BookingAlreadyCancelledException(String message) {
    super(message);
  }

  public BookingAlreadyCancelledException(String message, Throwable ex) {
    super(message, ex);
  }
}
