package com.cjrequena.sample.query.handler.domain.exception;


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
