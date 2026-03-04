package com.cjrequena.sample.command.handler.service.command;

import com.cjrequena.sample.command.handler.TestBase;
import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.domain.model.command.CreateBookingCommand;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.command.handler.domain.model.vo.PaxVO;
import com.cjrequena.sample.command.handler.domain.model.vo.ProductVO;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CreateBookingCommandHandler Tests")
class CreateBookingCommandHandlerTest extends TestBase {

  @Mock
  private EventStoreService eventStoreService;

  @Mock
  private EventMapper eventMapper;

  @Mock
  private EventStoreConfigurationProperties eventStoreConfigurationProperties;

  @InjectMocks
  private CreateBookingCommandHandler handler;

  private CreateBookingCommand command;
  private UUID leadPaxId;
  private List<PaxVO> paxes;
  private List<ProductVO> products;

  @BeforeEach
  void setUp() {
    leadPaxId = UUID.randomUUID();
    paxes = List.of(
      PaxVO.builder()
        .paxId(leadPaxId)
        .firstName("John")
        .lastName("Doe")
        .email("john@example.com")
        .phone("+1234567890")
        .age(30)
        .documentType("PASSPORT")
        .documentNumber("AB123456")
        .paxType("ADULT")
        .build()
    );
    products = List.of();

    command = CreateBookingCommand.builder()
      .paxes(paxes)
      .leadPaxId(leadPaxId)
      .products(products)
      .metadata(Map.of("source", "test"))
      .build();

    // Mock snapshot configuration
    EventStoreConfigurationProperties.SnapshotProperties snapshotProps =
      new EventStoreConfigurationProperties.SnapshotProperties(false, 10);
    when(eventStoreConfigurationProperties.getSnapshot(anyString())).thenReturn(snapshotProps);
    when(eventStoreService.retrieveEventsByAggregateId(any(), any(), any())).thenReturn(List.of());
    when(eventMapper.toEventList(any())).thenReturn(List.of());
  }

  @Test
  @DisplayName("Should handle CreateBookingCommand successfully")
  void shouldHandleCreateBookingCommand() throws Exception {
    Aggregate result = handler.handle(command);

    assertNotNull(result);
    assertTrue(result instanceof Booking);
    verify(eventStoreService).saveAggregate(any(Aggregate.class));
  }

  @Test
  @DisplayName("Should return correct command type")
  void shouldReturnCorrectCommandType() {
    assertEquals(CreateBookingCommand.class, handler.getCommandType());
  }

  @Test
  @DisplayName("Should return correct aggregate type")
  void shouldReturnCorrectAggregateType() {
    assertEquals(AggregateType.BOOKING_ORDER, handler.getAggregateType());
  }

  @Test
  @DisplayName("Should throw exception for wrong command type")
  void shouldThrowExceptionForWrongCommandType() {
    // Create a mock command of wrong type
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
}
