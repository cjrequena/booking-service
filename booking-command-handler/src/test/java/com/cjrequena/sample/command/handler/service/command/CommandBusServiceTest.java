package com.cjrequena.sample.command.handler.service.command;

import com.cjrequena.sample.command.handler.domain.exception.CommandHandlerNotFoundException;
import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.domain.model.command.CreateBookingCommand;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.command.handler.domain.model.vo.PaxVO;
import com.cjrequena.sample.command.handler.service.projection.ProjectionHandler;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CommandBusService Tests")
class CommandBusServiceTest {

  @Mock
  private CommandHandler<CreateBookingCommand> commandHandler;

  @Mock
  private ProjectionHandler projectionHandler;

  private CommandBusService commandBusService;
  private CreateBookingCommand command;
  private Booking mockAggregate;

  @BeforeEach
  void setUp() {
    UUID leadPaxId = UUID.randomUUID();
    List<PaxVO> paxes = List.of(
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

    command = CreateBookingCommand.builder()
      .paxes(paxes)
      .leadPaxId(leadPaxId)
      .products(List.of())
      .metadata(Map.of("source", "test"))
      .build();

    mockAggregate = Booking.builder()
      .aggregateId(command.getAggregateId())
      .aggregateVersion(0L)
      .build();

    // Setup mocks
    when(commandHandler.getCommandType()).thenReturn(CreateBookingCommand.class);
    when(commandHandler.handle(any(Command.class))).thenReturn(mockAggregate);
    when(projectionHandler.getAggregateType()).thenReturn(AggregateType.BOOKING_ORDER);

    // Create service with mocked dependencies
    commandBusService = new CommandBusService(
      List.of(commandHandler),
      List.of(projectionHandler)
    );
    
    // Set the enabled flag using reflection
    ReflectionTestUtils.setField(commandBusService, "projectionsHandlersEnabled", true);
  }

  @Test
  @DisplayName("Should handle command successfully")
  void shouldHandleCommandSuccessfully() {
    Aggregate result = commandBusService.handle(command);

    assertNotNull(result);
    assertEquals(mockAggregate, result);
    verify(commandHandler).handle(command);
  }

  @Test
  @DisplayName("Should invoke projection handler when enabled")
  void shouldInvokeProjectionHandlerWhenEnabled() {
    commandBusService.handle(command);

    verify(projectionHandler).handle(mockAggregate);
  }

  @Test
  @DisplayName("Should not invoke projection handler when disabled")
  void shouldNotInvokeProjectionHandlerWhenDisabled() {
    ReflectionTestUtils.setField(commandBusService, "projectionsHandlersEnabled", false);

    commandBusService.handle(command);

    verify(projectionHandler, never()).handle(any());
  }

  @Test
  @DisplayName("Should throw exception when no handler found")
  void shouldThrowExceptionWhenNoHandlerFound() {
    // Create a service with no handlers
    CommandBusService emptyService = new CommandBusService(List.of(), List.of());
    
    assertThrows(CommandHandlerNotFoundException.class, () -> emptyService.handle(command));
  }

  @Test
  @DisplayName("Should find correct handler by command type")
  void shouldFindCorrectHandlerByCommandType() {
    Aggregate result = commandBusService.handle(command);

    assertNotNull(result);
    verify(commandHandler, times(1)).handle(command);
  }

  @Test
  @DisplayName("Should filter projection handlers by aggregate type")
  void shouldFilterProjectionHandlersByAggregateType() {
    // Create a projection handler for different aggregate type
    ProjectionHandler otherHandler = mock(ProjectionHandler.class);
    when(otherHandler.getAggregateType()).thenReturn(AggregateType.BOOKING_ORDER);

    CommandBusService service = new CommandBusService(
      List.of(commandHandler),
      List.of(projectionHandler, otherHandler)
    );
    ReflectionTestUtils.setField(service, "projectionsHandlersEnabled", true);

    service.handle(command);

    // Both handlers should be invoked since they match the aggregate type
    verify(projectionHandler).handle(mockAggregate);
    verify(otherHandler).handle(mockAggregate);
  }
}
