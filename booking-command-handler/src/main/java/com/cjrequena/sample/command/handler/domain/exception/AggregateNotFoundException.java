package com.cjrequena.sample.command.handler.domain.exception;

/**
 *
 * <p></p>
 * <p></p>
 * @author cjrequena
 */
public class AggregateNotFoundException extends RuntimeException {
  public AggregateNotFoundException(String message) {
    super(message);
  }
}
