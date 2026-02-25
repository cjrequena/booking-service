package com.cjrequena.sample.query.handler.domain.exception;

/**
 *
 * <p></p>
 * <p></p>
 * @author cjrequena
 */
public class BookingNotFoundException extends DomainRuntimeException {
  public BookingNotFoundException(String message) {
    super(message);
  }

  public BookingNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
