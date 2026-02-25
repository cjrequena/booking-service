package com.cjrequena.sample.command.handler.controller.exception;

import org.springframework.http.HttpStatus;

/**
 *
 * <p></p>
 * <p></p>
 * @author cjrequena
 */
public class BadRequestException extends ControllerRuntimeException {

  public BadRequestException() {
    super(HttpStatus.BAD_REQUEST);
  }

  public BadRequestException(String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }

  public BadRequestException(String message, Throwable throwable) {
    super(HttpStatus.BAD_REQUEST, message, throwable);
  }

}
