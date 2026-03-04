package com.cjrequena.sample.command.handler.service.projection;

import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.command.handler.domain.model.enums.BookingStatus;
import com.cjrequena.sample.command.handler.persistence.mongodb.entity.BookingEntity;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BookingProjectionHandler Tests")
class BookingProjectionHandlerTest {

  @Mock
  private BookingProjectionService bookingProjectionService;

  @InjectMocks
  private BookingProjectionHandler handler;

  private Booking aggregate;
  private BookingEntity bookingEntity;

  @BeforeEach
  void setUp() {
    UUID bookingId = UUID.randomUUID();

    aggregate = mock(Booking.class);
    when(aggregate.getBookingId()).thenReturn(bookingId);
    when(aggregate.getBookingReference()).thenReturn("BK-12345");
    when(aggregate.getStatus()).thenReturn(BookingStatus.CREATED);
    when(aggregate.getPaxes()).thenReturn(List.of());
    when(aggregate.getLeadPaxId()).thenReturn(UUID.randomUUID());
    when(aggregate.getProducts()).thenReturn(List.of());
    when(aggregate.getMetadata()).thenReturn(Map.of());

    bookingEntity = BookingEntity.builder()
      .bookingId(bookingId)
      .bookingReference("BK-12345")
      .status(BookingStatus.CREATED)
      .build();

    when(bookingProjectionService.save(any(Booking.class))).thenReturn(bookingEntity);
  }

  @Test
  @DisplayName("Should handle aggregate and save to projection")
  void shouldHandleAggregateAndSaveToProjection() {
    handler.handle(aggregate);

    verify(bookingProjectionService).save(aggregate);
  }

  @Test
  @DisplayName("Should return correct aggregate type")
  void shouldReturnCorrectAggregateType() {
    assertEquals(AggregateType.BOOKING_ORDER, handler.getAggregateType());
  }

  @Test
  @DisplayName("Should cast aggregate to Booking before saving")
  void shouldCastAggregateToBookingBeforeSaving() {
    handler.handle(aggregate);

    verify(bookingProjectionService).save(any(Booking.class));
  }

  @Test
  @DisplayName("Should handle aggregate successfully")
  void shouldHandleAggregateSuccessfully() {
    assertDoesNotThrow(() -> handler.handle(aggregate));
  }
}
