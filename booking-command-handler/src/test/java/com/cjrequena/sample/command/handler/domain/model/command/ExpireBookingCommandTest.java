package com.cjrequena.sample.command.handler.domain.model.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExpireBookingCommand Tests")
class ExpireBookingCommandTest {

  @Test
  @DisplayName("Should create command with booking ID")
  void shouldCreateCommandWithBookingId() {
    UUID bookingId = UUID.randomUUID();

    ExpireBookingCommand command = ExpireBookingCommand.builder()
      .bookingId(bookingId)
      .build();

    assertNotNull(command);
    assertEquals(bookingId, command.getAggregateId());
  }

  @Test
  @DisplayName("Should have aggregate type set")
  void shouldHaveAggregateTypeSet() {
    ExpireBookingCommand command = ExpireBookingCommand.builder()
      .bookingId(UUID.randomUUID())
      .build();

    assertNotNull(command.getAggregateType());
    assertFalse(command.getAggregateType().isEmpty());
  }

  @Test
  @DisplayName("Should have meaningful toString representation")
  void shouldHaveMeaningfulToString() {
    ExpireBookingCommand command = ExpireBookingCommand.builder()
      .bookingId(UUID.randomUUID())
      .build();

    String toString = command.toString();
    assertNotNull(toString);
    assertTrue(toString.contains("ExpireBookingCommand"));
  }

  @Test
  @DisplayName("Should create multiple distinct commands")
  void shouldCreateMultipleDistinctCommands() {
    UUID bookingId1 = UUID.randomUUID();
    UUID bookingId2 = UUID.randomUUID();

    ExpireBookingCommand command1 = ExpireBookingCommand.builder()
      .bookingId(bookingId1)
      .build();

    ExpireBookingCommand command2 = ExpireBookingCommand.builder()
      .bookingId(bookingId2)
      .build();

    assertNotEquals(command1.getAggregateId(), command2.getAggregateId());
  }
}
