package com.cjrequena.sample.command.handler.domain.exception;


public class JsonUtilException extends DomainRuntimeException {

  public JsonUtilException(Throwable ex) {
    super(ex);
  }

  public JsonUtilException(String message) {
    super(message);
  }

  public JsonUtilException(String message, Throwable ex) {
    super(message, ex);
  }
}
