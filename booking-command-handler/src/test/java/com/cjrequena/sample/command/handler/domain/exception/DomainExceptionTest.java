package com.cjrequena.sample.command.handler.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Exception Tests")
class DomainExceptionTest {

  @Test
  @DisplayName("Should create AggregateNotFoundException with message")
  void shouldCreateAggregateNotFoundException() {
    String message = "Aggregate not found with ID: 123";
    AggregateNotFoundException exception = new AggregateNotFoundException(message);

    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
  }

  @Test
  @DisplayName("Should create BookingAlreadyCancelledException with throwable")
  void shouldCreateBookingAlreadyCancelledExceptionWithThrowable() {
    Throwable cause = new RuntimeException("Original cause");
    BookingAlreadyCancelledException exception = new BookingAlreadyCancelledException(cause);

    assertNotNull(exception);
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("Should create BookingAlreadyCancelledException with message")
  void shouldCreateBookingAlreadyCancelledExceptionWithMessage() {
    String message = "Booking already cancelled";
    BookingAlreadyCancelledException exception = new BookingAlreadyCancelledException(message);

    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
  }

  @Test
  @DisplayName("Should create BookingProjectionException with message")
  void shouldCreateBookingProjectionException() {
    String message = "Projection update failed";
    BookingProjectionException exception = new BookingProjectionException(message);

    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
  }

  @Test
  @DisplayName("Should create BookingProjectionException with message and cause")
  void shouldCreateBookingProjectionExceptionWithCause() {
    String message = "Projection update failed";
    Throwable cause = new RuntimeException("Database error");
    BookingProjectionException exception = new BookingProjectionException(message, cause);

    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("Should create CommandHandlerNotFoundException with message")
  void shouldCreateCommandHandlerNotFoundException() {
    String message = "Command handler not found for command type";
    CommandHandlerNotFoundException exception = new CommandHandlerNotFoundException(message);

    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
  }

  @Test
  @DisplayName("Should create InvalidArgumentException with message")
  void shouldCreateInvalidArgumentException() {
    String message = "Invalid argument provided";
    InvalidArgumentException exception = new InvalidArgumentException(message);

    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
  }

  @Test
  @DisplayName("Should create MapperException with throwable")
  void shouldCreateMapperExceptionWithThrowable() {
    Throwable cause = new RuntimeException("Mapping failed");
    MapperException exception = new MapperException(cause);

    assertNotNull(exception);
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("Should create MapperException with message")
  void shouldCreateMapperExceptionWithMessage() {
    String message = "Mapping error occurred";
    MapperException exception = new MapperException(message);

    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
  }

  @Test
  @DisplayName("Should create OptimisticConcurrencyException with message")
  void shouldCreateOptimisticConcurrencyException() {
    String message = "Optimistic concurrency conflict detected";
    OptimisticConcurrencyException exception = new OptimisticConcurrencyException(message);

    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
  }

  @Test
  @DisplayName("Should create PaxPriceException with message")
  void shouldCreatePaxPriceException() {
    String message = "Invalid pax price calculation";
    PaxPriceException exception = new PaxPriceException(message);

    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
  }

  @Test
  @DisplayName("Should verify exception inheritance hierarchy")
  void shouldVerifyExceptionInheritance() {
    // Specific exceptions extend DomainRuntimeException
    assertTrue(new OptimisticConcurrencyException("test") instanceof DomainRuntimeException);
    assertTrue(new InvalidArgumentException("test") instanceof DomainRuntimeException);
    assertTrue(new CommandHandlerNotFoundException("test") instanceof DomainRuntimeException);
    assertTrue(new BookingAlreadyCancelledException("test") instanceof DomainRuntimeException);
    assertTrue(new MapperException("test") instanceof DomainRuntimeException);
    assertTrue(new PaxPriceException("test") instanceof DomainRuntimeException);

    // Other exceptions extend RuntimeException directly
    assertTrue(new AggregateNotFoundException("test") instanceof RuntimeException);
    assertTrue(new BookingProjectionException("test") instanceof RuntimeException);
  }

  @Test
  @DisplayName("Should be throwable and catchable")
  void shouldBeThrowableAndCatchable() {
    assertThrows(OptimisticConcurrencyException.class, () -> {
      throw new OptimisticConcurrencyException("Concurrency conflict");
    });

    assertThrows(AggregateNotFoundException.class, () -> {
      throw new AggregateNotFoundException("Aggregate not found");
    });
  }

  @Test
  @DisplayName("Should catch specific exception in hierarchy")
  void shouldCatchSpecificExceptionInHierarchy() {
    try {
      throw new OptimisticConcurrencyException("Conflict");
    } catch (DomainRuntimeException e) {
      assertTrue(e instanceof OptimisticConcurrencyException);
      assertEquals("Conflict", e.getMessage());
    }
  }
}
