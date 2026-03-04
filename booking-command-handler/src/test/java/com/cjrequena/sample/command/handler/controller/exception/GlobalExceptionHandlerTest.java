package com.cjrequena.sample.command.handler.controller.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

  @InjectMocks
  private GlobalExceptionHandler exceptionHandler;

  @Test
  @DisplayName("Should handle ControllerException")
  void shouldHandleControllerException() {
    // Create a concrete implementation of ControllerException for testing
    ControllerException exception = new ControllerException(HttpStatus.BAD_REQUEST, "Bad request") {};

    ResponseEntity<ErrorDTO> response = exceptionHandler.handleControllerException(exception);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Bad request", response.getBody().getMessage());
    assertEquals(400, response.getBody().getStatus());
  }

  @Test
  @DisplayName("Should handle ControllerRuntimeException")
  void shouldHandleControllerRuntimeException() {
    ControllerRuntimeException exception = new ConflictException("Conflict");

    ResponseEntity<ErrorDTO> response = exceptionHandler.handleControllerRuntimeException(exception);

    assertNotNull(response);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Conflict", response.getBody().getMessage());
    assertEquals(409, response.getBody().getStatus());
  }

  @Test
  @DisplayName("Should handle MethodArgumentNotValidException")
  void shouldHandleMethodArgumentNotValidException() {
    MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("object", "field", "rejectedValue", false, null, null, "Field is required");

    when(exception.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<ErrorDTO> response = exceptionHandler.handleValidationException(exception);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Validation failed for one or more fields", response.getBody().getMessage());
    assertNotNull(response.getBody().getValidationErrors());
    assertEquals(1, response.getBody().getValidationErrors().size());
    assertEquals("field", response.getBody().getValidationErrors().get(0).getField());
  }

  @Test
  @DisplayName("Should handle IllegalArgumentException")
  void shouldHandleIllegalArgumentException() {
    IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

    ResponseEntity<ErrorDTO> response = exceptionHandler.handleIllegalArgumentException(exception);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Invalid argument", response.getBody().getMessage());
  }

  @Test
  @DisplayName("Should handle IllegalStateException")
  void shouldHandleIllegalStateException() {
    IllegalStateException exception = new IllegalStateException("Invalid state");

    ResponseEntity<ErrorDTO> response = exceptionHandler.handleIllegalStateException(exception);

    assertNotNull(response);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Invalid state", response.getBody().getMessage());
  }

  @Test
  @DisplayName("Should handle generic Exception")
  void shouldHandleGenericException() {
    Exception exception = new Exception("Unexpected error");

    ResponseEntity<ErrorDTO> response = exceptionHandler.handleGenericException(exception);

    assertNotNull(response);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("An unexpected error occurred", response.getBody().getMessage());
  }

  @Test
  @DisplayName("Should handle HttpMessageNotReadableException with generic message")
  void shouldHandleHttpMessageNotReadableException() {
    HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
    when(exception.getCause()).thenReturn(null);

    ResponseEntity<ErrorDTO> response = exceptionHandler.handleHttpMessageNotReadable(exception);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Malformed JSON request or invalid request body.", response.getBody().getMessage());
  }
}
