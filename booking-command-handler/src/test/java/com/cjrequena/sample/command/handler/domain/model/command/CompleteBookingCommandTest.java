package com.cjrequena.sample.command.handler.domain.model.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CompleteBookingCommand Tests")
class CompleteBookingCommandTest {

  @Test
  @DisplayName("Should create command with aggregate ID")
  void shouldCreateCommandWithAggregateId() {
    UUID bookingId = UUID.randomUUID();

    CompleteBookingCommand command = CompleteBookingCommand.builder()
      .bookingId(bookingId)
      .build();

    assertNotNull(command);
    assertEquals(bookingId, command.getAggregateId());
  }

  @Test
  @DisplayName("Should have aggregate type set")
  void shouldHaveAggregateTypeSet() {
    CompleteBookingCommand command = CompleteBookingCommand.builder()
      .bookingId(UUID.randomUUID())
      .build();

    assertNotNull(command.getAggregateType());
    assertFalse(command.getAggregateType().isEmpty());
  }

  @Test
  @DisplayName("Should have meaningful toString representation")
  void shouldHaveMeaningfulToString() {
    CompleteBookingCommand command = CompleteBookingCommand.builder()
      .bookingId(UUID.randomUUID())
      .build();

    String toString = command.toString();
    assertNotNull(toString);
    assertTrue(toString.contains("CompleteBookingCommand"));
  }

  @Test
  @DisplayName("Should create multiple distinct commands")
  void shouldCreateMultipleDistinctCommands() {
    UUID bookingId1 = UUID.randomUUID();
    UUID bookingId2 = UUID.randomUUID();

    CompleteBookingCommand command1 = CompleteBookingCommand.builder()
      .bookingId(bookingId1)
      .build();

    CompleteBookingCommand command2 = CompleteBookingCommand.builder()
      .bookingId(bookingId2)
      .build();

    assertNotEquals(command1.getAggregateId(), command2.getAggregateId());
  }
}
