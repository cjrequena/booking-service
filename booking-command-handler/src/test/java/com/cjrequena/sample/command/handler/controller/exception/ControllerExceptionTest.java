package com.cjrequena.sample.command.handler.controller.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Controller Exception Tests")
class ControllerExceptionTest {

  @Test
  @DisplayName("BadRequestException should have correct status")
  void badRequestExceptionShouldHaveCorrectStatus() {
    BadRequestException exception = new BadRequestException("Bad request");
    
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals("Bad request", exception.getMessage());
  }

  @Test
  @DisplayName("BadRequestException with cause should preserve throwable")
  void badRequestExceptionWithCauseShouldPreserveThrowable() {
    Throwable cause = new RuntimeException("Root cause");
    BadRequestException exception = new BadRequestException("Bad request", cause);
    
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals("Bad request", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("ConflictException should have correct status")
  void conflictExceptionShouldHaveCorrectStatus() {
    ConflictException exception = new ConflictException("Conflict");
    
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    assertEquals("Conflict", exception.getMessage());
  }

  @Test
  @DisplayName("NotFoundException should have correct status")
  void notFoundExceptionShouldHaveCorrectStatus() {
    NotFoundException exception = new NotFoundException("Not found");
    
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    assertEquals("Not found", exception.getMessage());
  }

  @Test
  @DisplayName("NotAcceptableException should have correct status")
  void notAcceptableExceptionShouldHaveCorrectStatus() {
    NotAcceptableException exception = new NotAcceptableException();
    
    assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
  }

  @Test
  @DisplayName("NotImplementedException should have correct status")
  void notImplementedExceptionShouldHaveCorrectStatus() {
    NotImplementedException exception = new NotImplementedException("Not implemented");
    
    assertEquals(HttpStatus.NOT_IMPLEMENTED, exception.getHttpStatus());
    assertEquals("Not implemented", exception.getMessage());
  }

  @Test
  @DisplayName("ControllerException default constructor should work")
  void controllerExceptionDefaultConstructorShouldWork() {
    BadRequestException exception = new BadRequestException();
    
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }
}
