package com.cjrequena.sample.command.handler.service.command;

import com.cjrequena.sample.command.handler.TestBase;
import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.domain.model.command.CancelBookingCommand;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CancelBookingCommandHandler Tests")
class CancelBookingCommandHandlerTest extends TestBase {

  @Mock
  private EventStoreService eventStoreService;

  @Mock
  private EventMapper eventMapper;

  @Mock
  private EventStoreConfigurationProperties eventStoreConfigurationProperties;

  @InjectMocks
  private CancelBookingCommandHandler handler;

  private CancelBookingCommand command;

  @BeforeEach
  void setUp() {
    command = CancelBookingCommand.builder()
      .bookingId(UUID.randomUUID())
      .build();

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
  @DisplayName("Should handle CancelBookingCommand successfully")
  void shouldHandleCancelBookingCommand() throws Exception {
    Aggregate result = handler.handle(command);

    assertNotNull(result);
    assertTrue(result instanceof Booking);
    verify(eventStoreService).saveAggregate(any(Aggregate.class));
  }

  @Test
  @DisplayName("Should return correct command type")
  void shouldReturnCorrectCommandType() {
    assertEquals(CancelBookingCommand.class, handler.getCommandType());
  }

  @Test
  @DisplayName("Should return correct aggregate type")
  void shouldReturnCorrectAggregateType() {
    assertEquals(AggregateType.BOOKING_ORDER, handler.getAggregateType());
  }
}
