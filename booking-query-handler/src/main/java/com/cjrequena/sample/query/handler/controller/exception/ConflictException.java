package com.cjrequena.sample.query.handler.controller.exception;

import org.springframework.http.HttpStatus;

/**
 * @author cjrequena
 *
 */
public class ConflictException extends ControllerException {

  public ConflictException() {
    super(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase());
  }

  public ConflictException(String message) {
    super(HttpStatus.CONFLICT, message);
  }
}
