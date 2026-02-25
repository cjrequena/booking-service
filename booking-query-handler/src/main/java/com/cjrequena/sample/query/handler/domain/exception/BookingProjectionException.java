package com.cjrequena.sample.query.handler.domain.exception;

/**
 *
 * <p></p>
 * <p></p>
 * @author cjrequena
 */
public class BookingProjectionException extends DomainRuntimeException {
  public BookingProjectionException(String message) {
    super(message);
  }

  public BookingProjectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
