package com.cjrequena.sample.command.handler.service.command;

import com.cjrequena.sample.command.handler.TestBase;
import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.domain.model.command.ConfirmBookingCommand;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.service.EventStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConfirmBookingCommandHandler Tests")
class ConfirmBookingCommandHandlerTest extends TestBase {

  @Mock
  private EventStoreService eventStoreService;

  @Mock
  private EventMapper eventMapper;

  @Mock
  private EventStoreConfigurationProperties eventStoreConfigurationProperties;

  @InjectMocks
  private ConfirmBookingCommandHandler handler;

  private ConfirmBookingCommand command;
  private UUID bookingId;

  @BeforeEach
  void setUp() {
    bookingId = UUID.randomUUID();

    command = ConfirmBookingCommand.builder()
      .bookingId(bookingId)
      .build();

    // Mock snapshot configuration
    EventStoreConfigurationProperties.SnapshotProperties snapshotProps =
      new EventStoreConfigurationProperties.SnapshotProperties(false, 10);
    when(eventStoreConfigurationProperties.getSnapshot(anyString())).thenReturn(snapshotProps);

    // Mock that the aggregate exists
    when(eventStoreService.verifyIfAggregateExist(any(UUID.class), anyString())).thenReturn(true);

    // Mock event retrieval - return empty list since we'll mock the aggregate directly
    when(eventStoreService.retrieveEventsByAggregateId(any(), any(), any())).thenReturn(List.of());
    when(eventMapper.toEventList(any())).thenReturn(List.of());
  }

  @Test
  @DisplayName("Should handle ConfirmBookingCommand successfully")
  void shouldHandleConfirmBookingCommand() throws Exception {
    Aggregate result = handler.handle(command);

    assertNotNull(result);
    assertTrue(result instanceof Booking);
    verify(eventStoreService).saveAggregate(any(Aggregate.class));
  }

  @Test
  @DisplayName("Should return correct command type")
  void shouldReturnCorrectCommandType() {
    assertEquals(ConfirmBookingCommand.class, handler.getCommandType());
  }

  @Test
  @DisplayName("Should return correct aggregate type")
  void shouldReturnCorrectAggregateType() {
    assertEquals(AggregateType.BOOKING_ORDER, handler.getAggregateType());
  }

  @Test
  @DisplayName("Should throw exception for wrong command type")
  void shouldThrowExceptionForWrongCommandType() {
    var wrongCommand = mock(com.cjrequena.sample.es.core.domain.model.command.Command.class);
    when(wrongCommand.getAggregateId()).thenReturn(UUID.randomUUID());
    when(wrongCommand.getAggregateType()).thenReturn("BOOKING_ORDER");

    assertThrows(IllegalArgumentException.class, () -> handler.handle(wrongCommand));
  }

  @Test
  @DisplayName("Should save aggregate after applying command")
  void shouldSaveAggregateAfterApplyingCommand() throws Exception {
    handler.handle(command);

    verify(eventStoreService, times(1)).saveAggregate(any(Aggregate.class));
  }

  @Test
  @DisplayName("Should mark events as confirmed after save")
  void shouldMarkEventsAsConfirmedAfterSave() throws Exception {
    Aggregate result = handler.handle(command);

    assertTrue(result.getUnconfirmedEventsPool().isEmpty());
  }

  @Test
  @DisplayName("Should retrieve existing aggregate by ID")
  void shouldRetrieveExistingAggregateById() throws Exception {
    handler.handle(command);

    verify(eventStoreService).retrieveEventsByAggregateId(eq(bookingId), any(), any());
  }
}
