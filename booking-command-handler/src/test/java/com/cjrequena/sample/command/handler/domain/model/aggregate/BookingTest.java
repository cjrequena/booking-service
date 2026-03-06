package com.cjrequena.sample.command.handler.domain.model.aggregate;

import com.cjrequena.sample.command.handler.TestBase;
import com.cjrequena.sample.command.handler.domain.model.command.*;
import com.cjrequena.sample.command.handler.domain.model.enums.BookingStatus;
import com.cjrequena.sample.command.handler.domain.model.event.*;
import com.cjrequena.sample.command.handler.domain.model.vo.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Booking Aggregate Tests")
class BookingTest extends TestBase {

  private UUID aggregateId;
  private UUID paxId;
  private UUID leadPaxId;
  private String bookingReference;
  private List<PaxVO> paxes;
  private List<ProductVO> products;

  @BeforeEach
  void setUp() {
    aggregateId = UUID.randomUUID();
    paxId = UUID.randomUUID();
    leadPaxId = UUID.randomUUID();
    bookingReference = "BK-" + UUID.randomUUID().toString().substring(0, 8);

    paxes = List.of(
      PaxVO.builder()
        .paxId(paxId)
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .phone("+1234567890")
        .age(30)
        .documentType("PASSPORT")
        .documentNumber("AB123456")
        .paxType("ADULT")
        .build(),
      PaxVO.builder()
        .paxId(leadPaxId)
        .firstName("Jane")
        .lastName("Doe")
        .email("jane.doe@example.com")
        .phone("+1234567891")
        .age(28)
        .documentType("PASSPORT")
        .documentNumber("CD789012")
        .paxType("ADULT")
        .build()
    );

    products = List.of();
  }

  @Test
  @DisplayName("Should create booking aggregate with valid parameters")
  void shouldCreateBookingAggregate() {
    Booking booking = Booking.builder()
      .aggregateId(aggregateId)
      .aggregateVersion(0L)
      .build();

    assertNotNull(booking);
    assertEquals(aggregateId, booking.getAggregateId());
    assertEquals(0L, booking.getAggregateVersion());
  }

  @Test
  @DisplayName("Should apply CreateBookingCommand and generate BookingCreatedEvent")
  void shouldApplyCreateBookingCommand() throws JsonProcessingException {
    Booking booking = Booking.builder()
      .aggregateId(aggregateId)
      .aggregateVersion(0L)
      .build();

    CreateBookingCommand command = CreateBookingCommand.builder()
      .paxes(paxes)
      .leadPaxId(leadPaxId)
      .products(products)
      .metadata(Map.of("source", "test"))
      .build();

    booking.applyCommand(command);

    assertEquals(1, booking.getUnconfirmedEventsPool().size());
    assertTrue(booking.getUnconfirmedEventsPool().get(0) instanceof BookingCreatedEvent);
  }

  @Test
  @DisplayName("Should apply BookingCreatedEvent and update aggregate state")
  void shouldApplyBookingCreatedEvent() {
    Booking booking = Booking.builder()
      .aggregateId(aggregateId)
      .aggregateVersion(0L)
      .build();

    BookingCreatedEvent event = BookingCreatedEvent.builder()
      .eventId(UUID.randomUUID())
      .aggregateId(aggregateId)
      .aggregateVersion(1L)
      .dataContentType("application/json")
      .data(BookingCreatedEventDataVO.builder()
        .bookingId(aggregateId)
        .bookingReference(bookingReference)
        .status(BookingStatus.CREATED)
        .paxes(paxes)
        .leadPaxId(leadPaxId)
        .products(products)
        .metadata(Map.of("source", "test"))
        .build())
      .build();

    booking.applyEvent(event);

    assertEquals(aggregateId, booking.getBookingId());
    assertEquals(bookingReference, booking.getBookingReference());
    assertEquals(BookingStatus.CREATED, booking.getStatus());
    assertEquals(paxes, booking.getPaxes());
    assertEquals(leadPaxId, booking.getLeadPaxId());
    assertEquals(products, booking.getProducts());
  }



  @Test
  @DisplayName("Should apply ConfirmBookingCommand and generate BookingConfirmedEvent")
  void shouldApplyConfirmBookingCommand() throws JsonProcessingException {
    Booking booking = createBookingWithCreatedStatus();

    ConfirmBookingCommand command = ConfirmBookingCommand.builder()
      .bookingId(aggregateId)
      .build();

    booking.applyCommand(command);

    assertEquals(1, booking.getUnconfirmedEventsPool().size());
    assertTrue(booking.getUnconfirmedEventsPool().get(0) instanceof BookingConfirmedEvent);
  }

  @Test
  @DisplayName("Should apply BookingConfirmedEvent and update status to CONFIRMED")
  void shouldApplyBookingConfirmedEvent() {
    Booking booking = createBookingWithCreatedStatus();

    BookingConfirmedEvent event = BookingConfirmedEvent.builder()
      .eventId(UUID.randomUUID())
      .aggregateId(aggregateId)
      .aggregateVersion(3L)
      .dataContentType("application/json")
      .data(BookingConfirmedEventDataVO.builder()
        .bookingId(aggregateId)
        .bookingReference(bookingReference)
        .status(BookingStatus.CONFIRMED)
        .build())
      .build();

    booking.applyEvent(event);

    assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
  }

  @Test
  @DisplayName("Should apply CancelBookingCommand and generate BookingCancelledEvent")
  void shouldApplyCancelBookingCommand() throws JsonProcessingException {
    Booking booking = createBookingWithCreatedStatus();

    CancelBookingCommand command = CancelBookingCommand.builder()
      .bookingId(aggregateId)
      .build();

    booking.applyCommand(command);

    assertEquals(1, booking.getUnconfirmedEventsPool().size());
    assertTrue(booking.getUnconfirmedEventsPool().get(0) instanceof BookingCancelledEvent);
  }

  @Test
  @DisplayName("Should apply BookingCancelledEvent and update status to CANCELLED")
  void shouldApplyBookingCancelledEvent() {
    Booking booking = createBookingWithCreatedStatus();

    BookingCancelledEvent event = BookingCancelledEvent.builder()
      .eventId(UUID.randomUUID())
      .aggregateId(aggregateId)
      .aggregateVersion(3L)
      .dataContentType("application/json")
      .data(BookingCancelledEventDataVO.builder()
        .bookingId(aggregateId)
        .bookingReference(bookingReference)
        .status(BookingStatus.CANCELLED)
        .build())
      .build();

    booking.applyEvent(event);

    assertEquals(BookingStatus.CANCELLED, booking.getStatus());
  }

  @Test
  @DisplayName("Should apply CompleteBookingCommand and generate BookingCompletedEvent")
  void shouldApplyCompleteBookingCommand() throws JsonProcessingException {
    Booking booking = createBookingWithConfirmedStatus();

    CompleteBookingCommand command = CompleteBookingCommand.builder()
      .bookingId(aggregateId)
      .build();

    booking.applyCommand(command);

    assertEquals(1, booking.getUnconfirmedEventsPool().size());
    assertTrue(booking.getUnconfirmedEventsPool().get(0) instanceof BookingCompletedEvent);
  }

  @Test
  @DisplayName("Should apply BookingCompletedEvent and update status to COMPLETED")
  void shouldApplyBookingCompletedEvent() {
    Booking booking = createBookingWithConfirmedStatus();

    BookingCompletedEvent event = BookingCompletedEvent.builder()
      .eventId(UUID.randomUUID())
      .aggregateId(aggregateId)
      .aggregateVersion(4L)
      .dataContentType("application/json")
      .data(BookingCompletedEventDataVO.builder()
        .bookingId(aggregateId)
        .bookingReference(bookingReference)
        .status(BookingStatus.COMPLETED)
        .build())
      .build();

    booking.applyEvent(event);

    assertEquals(BookingStatus.COMPLETED, booking.getStatus());
  }

  @Test
  @DisplayName("Should apply ExpireBookingCommand and generate BookingExpiredEvent")
  void shouldApplyExpireBookingCommand() throws JsonProcessingException {
    Booking booking = createBookingWithCreatedStatus();

    ExpireBookingCommand command = ExpireBookingCommand.builder()
      .bookingId(aggregateId)
      .build();

    booking.applyCommand(command);

    assertEquals(1, booking.getUnconfirmedEventsPool().size());
    assertTrue(booking.getUnconfirmedEventsPool().get(0) instanceof BookingExpiredEvent);
  }

  @Test
  @DisplayName("Should apply BookingExpiredEvent and update status to EXPIRED")
  void shouldApplyBookingExpiredEvent() {
    Booking booking = createBookingWithCreatedStatus();

    BookingExpiredEvent event = BookingExpiredEvent.builder()
      .eventId(UUID.randomUUID())
      .aggregateId(aggregateId)
      .aggregateVersion(3L)
      .dataContentType("application/json")
      .data(BookingExpiredEventDataVO.builder()
        .bookingId(aggregateId)
        .bookingReference(bookingReference)
        .status(BookingStatus.EXPIRED)
        .build())
      .build();

    booking.applyEvent(event);

    assertEquals(BookingStatus.EXPIRED, booking.getStatus());
  }

  @Test
  @DisplayName("Should return correct aggregate type")
  void shouldReturnCorrectAggregateType() {
    Booking booking = Booking.builder()
      .aggregateId(aggregateId)
      .aggregateVersion(0L)
      .build();

    assertNotNull(booking.getAggregateType());
    assertFalse(booking.getAggregateType().isEmpty());
  }

  // Helper methods

  private Booking createBookingWithCreatedStatus() {
    Booking booking = Booking.builder()
      .aggregateId(aggregateId)
      .aggregateVersion(1L)
      .build();

    BookingCreatedEvent event = BookingCreatedEvent.builder()
      .eventId(UUID.randomUUID())
      .aggregateId(aggregateId)
      .aggregateVersion(1L)
      .dataContentType("application/json")
      .data(BookingCreatedEventDataVO.builder()
        .bookingId(aggregateId)
        .bookingReference(bookingReference)
        .status(BookingStatus.CREATED)
        .paxes(paxes)
        .leadPaxId(leadPaxId)
        .products(products)
        .metadata(Map.of())
        .build())
      .build();

    booking.applyEvent(event);
    booking.markUnconfirmedEventsAsConfirmed();
    return booking;
  }

  private Booking createBookingWithConfirmedStatus() {
    Booking booking = createBookingWithCreatedStatus();

    BookingConfirmedEvent event = BookingConfirmedEvent.builder()
      .eventId(UUID.randomUUID())
      .aggregateId(aggregateId)
      .aggregateVersion(3L)
      .dataContentType("application/json")
      .data(BookingConfirmedEventDataVO.builder()
        .bookingId(aggregateId)
        .bookingReference(bookingReference)
        .status(BookingStatus.CONFIRMED)
        .build())
      .build();

    booking.applyEvent(event);
    booking.markUnconfirmedEventsAsConfirmed();
    return booking;
  }
}
