package com.cjrequena.sample.command.handler.domain.exception;

/**
 *
 * <p></p>
 * <p></p>
 * @author cjrequena
 */
public class BookingProjectionException extends RuntimeException {
  public BookingProjectionException(String message) {
    super(message);
  }

  public BookingProjectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
