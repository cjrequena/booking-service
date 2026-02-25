package com.cjrequena.sample.query.handler.controller.exception;

import org.springframework.http.HttpStatus;

/**
 *
 * <p></p>
 * <p></p>
 * @author cjrequena
 */
public class BadRequestException extends ControllerException {
  public BadRequestException() {
    super(HttpStatus.BAD_REQUEST);
  }

  public BadRequestException(String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }
}
