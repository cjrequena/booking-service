package com.cjrequena.sample.command.handler.domain.model.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfirmBookingCommand Tests")
class ConfirmBookingCommandTest {

  @Test
  @DisplayName("Should create command with booking ID")
  void shouldCreateCommandWithBookingId() {
    UUID bookingId = UUID.randomUUID();

    ConfirmBookingCommand command = ConfirmBookingCommand.builder()
      .bookingId(bookingId)
      .build();

    assertNotNull(command);
    assertEquals(bookingId, command.getAggregateId());
  }

  @Test
  @DisplayName("Should have aggregate type set")
  void shouldHaveAggregateTypeSet() {
    ConfirmBookingCommand command = ConfirmBookingCommand.builder()
      .bookingId(UUID.randomUUID())
      .build();

    assertNotNull(command.getAggregateType());
    assertFalse(command.getAggregateType().isEmpty());
  }

  @Test
  @DisplayName("Should have meaningful toString representation")
  void shouldHaveMeaningfulToString() {
    ConfirmBookingCommand command = ConfirmBookingCommand.builder()
      .bookingId(UUID.randomUUID())
      .build();

    String toString = command.toString();
    assertNotNull(toString);
    assertTrue(toString.contains("ConfirmBookingCommand"));
  }

  @Test
  @DisplayName("Should create multiple distinct commands")
  void shouldCreateMultipleDistinctCommands() {
    UUID bookingId1 = UUID.randomUUID();
    UUID bookingId2 = UUID.randomUUID();

    ConfirmBookingCommand command1 = ConfirmBookingCommand.builder()
      .bookingId(bookingId1)
      .build();

    ConfirmBookingCommand command2 = ConfirmBookingCommand.builder()
      .bookingId(bookingId2)
      .build();

    assertNotEquals(command1.getAggregateId(), command2.getAggregateId());
  }
}
