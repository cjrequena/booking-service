package com.cjrequena.sample.es.core.domain.exception;

/**
 *
 * <p></p>
 * <p></p>
 * @author cjrequena
 */
public class AggregateNotFoundException extends DomainRuntimeException {
  public AggregateNotFoundException(String message) {
    super(message);
  }
}
