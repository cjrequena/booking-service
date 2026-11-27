package com.cjrequena.sample.query.handler.controller.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.DatabindException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.cjrequena.sample.query.handler.shared.common.Constant.ISO_LOCAL_DATE_TIME;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * A downstream dependency timed out or returned a 5xx. This is transient and retryable, not a bug
   * in this service, so surface a 503 rather than a generic 500.
   */
  @ExceptionHandler({ResourceAccessException.class, HttpServerErrorException.class})
  @ResponseBody
  public ResponseEntity<ErrorDTO> handleDownstreamUnavailable(RestClientException ex) {
    log.warn("Downstream dependency unavailable/slow: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
      "A downstream service is temporarily unavailable; please retry.");
  }

  @ExceptionHandler({ControllerException.class})
  @ResponseBody
  public ResponseEntity<ErrorDTO> handleControllerException(ControllerException ex) {
    log.warn("Exception: {}", ex.getMessage());
    return buildErrorResponse(ex.getHttpStatus(), ex.getMessage());
  }

  @ExceptionHandler({ControllerRuntimeException.class})
  @ResponseBody
  public ResponseEntity<ErrorDTO> handleControllerRuntimeException(ControllerRuntimeException ex) {
    log.warn("Exception: {}", ex.getMessage());
    return buildErrorResponse(ex.getHttpStatus(), ex.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseBody
  public ResponseEntity<ErrorDTO> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    log.warn("Exception: {}", ex.getMessage());

    Throwable cause = ex.getCause();
    String message;

    if (cause instanceof tools.jackson.databind.exc.InvalidFormatException invalidFormatEx) {
      Class<?> targetType = invalidFormatEx.getTargetType();

      // Handle invalid Date/Time format
      if (targetType != null && java.time.temporal.Temporal.class.isAssignableFrom(targetType)) {
        String field = invalidFormatEx
          .getPath()
          .stream()
          .map(DatabindException.Reference::getPropertyName)
          .filter(Objects::nonNull)
          .reduce((a, b) -> a + "." + b)
          .orElse("unknown");

        String invalidValue = String.valueOf(invalidFormatEx.getValue());
        message = String.format("Invalid date/time format for field '%s': '%s . Expected format: '%s' ", field, invalidValue, ISO_LOCAL_DATE_TIME);

        return buildErrorResponse(HttpStatus.BAD_REQUEST ,message);
      }

      // Handle invalid Enum values
      if (targetType != null && targetType.isEnum()) {
        String field = invalidFormatEx
          .getPath()
          .stream()
          .map(DatabindException.Reference::getPropertyName)
          .filter(Objects::nonNull)
          .reduce((a, b) -> a + "." + b)
          .orElse("unknown");

        String invalidValue = String.valueOf(invalidFormatEx.getValue());
        String acceptedValues = Arrays
          .stream(targetType.getEnumConstants())
          .map(Object::toString)
          .collect(Collectors.joining(", "));

        message = String.format("Invalid value for enum field '%s': '%s'. Accepted values are: [%s].", field, invalidValue, acceptedValues);

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
      }
    }

    // Generic fallback for malformed JSON or other unreadable content
    message = "Malformed JSON request or invalid request body.";
    return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseBody
  public ResponseEntity<ErrorDTO> handleValidationException(MethodArgumentNotValidException ex) {
    // Field names only: the exception message carries the rejected values (emails, phones).
    log.warn("Validation error [fields={}]", ex.getBindingResult().getFieldErrors().stream()
      .map(FieldError::getField).distinct().collect(Collectors.joining(", ")));
    List<ValidationError> validationErrors = ex.getBindingResult()
      .getFieldErrors()
      .stream()
      .map(this::mapToValidationError)
      .collect(Collectors.toList());
    return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields", validationErrors);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorDTO> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("Illegal argument: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorDTO> handleIllegalStateException(IllegalStateException ex) {
    log.warn("Illegal state: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorDTO> handleNoResourceFoundException(NoResourceFoundException ex) {
    log.warn("No handler found for request: {}", ex.getResourcePath());
    return buildErrorResponse(HttpStatus.NOT_FOUND, "No endpoint found for the requested path");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDTO> handleGenericException(Exception ex) {
    Throwable cause = ex.getCause();
    if (cause instanceof IllegalStateException) {
      log.warn("Illegal state: {}", cause.getMessage());
      return buildErrorResponse(HttpStatus.CONFLICT, cause.getMessage());
    }
    if (cause instanceof IllegalArgumentException) {
      log.warn("Illegal argument: {}", cause.getMessage());
      return buildErrorResponse(HttpStatus.BAD_REQUEST, cause.getMessage());
    }
    log.error("Unexpected error occurred", ex);
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
  }

  private ResponseEntity<ErrorDTO> buildErrorResponse(HttpStatus status, String message) {
    return buildErrorResponse(status, status.getReasonPhrase(), message);
  }

  private ResponseEntity<ErrorDTO> buildErrorResponse(HttpStatus status, String errorCode, String message) {
    ErrorDTO error = ErrorDTO.builder()
      .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      .status(status.value())
      .errorCode(errorCode)
      .message(message)
      .build();
    return ResponseEntity.status(status).body(error);
  }

  private ResponseEntity<ErrorDTO> buildErrorResponse(HttpStatus status, String message, List<ValidationError> validationErrors) {
    ErrorDTO error = ErrorDTO.builder()
      .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      .status(status.value())
      .errorCode(status.getReasonPhrase())
      .message(message)
      .validationErrors(validationErrors)
      .build();
    return ResponseEntity.status(status).body(error);
  }

  private ValidationError mapToValidationError(FieldError fieldError) {
    return ValidationError.builder()
      .field(fieldError.getField())
      .message(fieldError.getDefaultMessage())
      .rejectedValue(fieldError.getRejectedValue())
      .build();
  }
}
