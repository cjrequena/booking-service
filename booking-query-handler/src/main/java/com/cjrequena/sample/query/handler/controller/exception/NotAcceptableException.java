package com.cjrequena.sample.query.handler.controller.exception;

import org.springframework.http.HttpStatus;

/**
 *
 * <p></p>
 * <p></p>
 * @author cjrequena
 */
public class NotAcceptableException extends ControllerException {
  public NotAcceptableException() {
    super(HttpStatus.NOT_ACCEPTABLE);
  }
}
