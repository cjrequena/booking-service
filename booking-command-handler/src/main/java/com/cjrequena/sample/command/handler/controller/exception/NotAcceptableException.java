package com.cjrequena.sample.command.handler.controller.exception;

import org.springframework.http.HttpStatus;

/**
 *
 * <p></p>
 * <p></p>
 * @author cjrequena
 */
public class NotAcceptableException extends ControllerRuntimeException {
  public NotAcceptableException() {
    super(HttpStatus.NOT_ACCEPTABLE);
  }
}
