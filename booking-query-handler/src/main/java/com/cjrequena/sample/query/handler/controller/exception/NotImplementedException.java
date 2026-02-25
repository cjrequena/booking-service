package com.cjrequena.sample.query.handler.controller.exception;

import org.springframework.http.HttpStatus;

/**
 *
 * <p></p>
 * <p></p>
 * @author cjrequena
 */
public class NotImplementedException extends ControllerException {
  public NotImplementedException() {
    super(HttpStatus.NOT_IMPLEMENTED);
  }

  public NotImplementedException(String message) {
    super(HttpStatus.NOT_IMPLEMENTED, message);
  }
}
