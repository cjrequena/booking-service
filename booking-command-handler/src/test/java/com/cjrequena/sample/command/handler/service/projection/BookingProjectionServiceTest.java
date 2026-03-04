package com.cjrequena.sample.command.handler.service.projection;

import com.cjrequena.sample.command.handler.domain.exception.BookingProjectionException;
import com.cjrequena.sample.command.handler.domain.mapper.PaxMapper;
import com.cjrequena.sample.command.handler.domain.mapper.ProductMapper;
import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.domain.model.enums.BookingStatus;
import com.cjrequena.sample.command.handler.domain.model.vo.PaxVO;
import com.cjrequena.sample.command.handler.persistence.mongodb.entity.BookingEntity;
import com.cjrequena.sample.command.handler.persistence.mongodb.entity.PaxEntity;
import com.cjrequena.sample.command.handler.persistence.mongodb.entity.ProductEntity;
import com.cjrequena.sample.command.handler.persistence.mongodb.repository.BookingProjectionRepository;
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
@DisplayName("BookingProjectionService Tests")
class BookingProjectionServiceTest {

  @Mock
  private BookingProjectionRepository bookingProjectionRepository;

  @Mock
  private PaxMapper paxMapper;

  @Mock
  private ProductMapper productMapper;

  @InjectMocks
  private BookingProjectionService service;

  private Booking aggregate;
  private BookingEntity bookingEntity;
  private UUID bookingId;
  private UUID leadPaxId;
  private List<PaxVO> paxes;
  private List<PaxEntity> paxEntities;
  private List<ProductEntity> productEntities;

  @BeforeEach
  void setUp() {
    bookingId = UUID.randomUUID();
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

    paxEntities = List.of(
      PaxEntity.builder()
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

    productEntities = List.of();

    aggregate = Booking.builder()
      .aggregateId(bookingId)
      .aggregateVersion(1L)
      .build();

    // Manually set fields using reflection or create a test helper
    // For now, we'll mock the aggregate methods
    aggregate = mock(Booking.class);
    when(aggregate.getBookingId()).thenReturn(bookingId);
    when(aggregate.getBookingReference()).thenReturn("BK-12345");
    when(aggregate.getStatus()).thenReturn(BookingStatus.CREATED);
    when(aggregate.getPaxes()).thenReturn(paxes);
    when(aggregate.getLeadPaxId()).thenReturn(leadPaxId);
    when(aggregate.getProducts()).thenReturn(List.of());
    when(aggregate.getMetadata()).thenReturn(Map.of("source", "test"));

    bookingEntity = BookingEntity.builder()
      .bookingId(bookingId)
      .bookingReference("BK-12345")
      .status(BookingStatus.CREATED)
      .paxes(paxEntities)
      .leadPaxId(leadPaxId)
      .products(productEntities)
      .metadata(Map.of("source", "test"))
      .build();

    when(paxMapper.toPaxList(any())).thenReturn(paxEntities);
    when(productMapper.toProductList(any())).thenReturn(productEntities);
    when(bookingProjectionRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
  }

  @Test
  @DisplayName("Should save booking aggregate to projection database")
  void shouldSaveBookingAggregateToProjectionDatabase() {
    BookingEntity result = service.save(aggregate);

    assertNotNull(result);
    assertEquals(bookingId, result.getBookingId());
    assertEquals("BK-12345", result.getBookingReference());
    assertEquals(BookingStatus.CREATED, result.getStatus());
    verify(bookingProjectionRepository).save(any(BookingEntity.class));
  }

  @Test
  @DisplayName("Should map paxes using PaxMapper")
  void shouldMapPaxesUsingPaxMapper() {
    service.save(aggregate);

    verify(paxMapper).toPaxList(paxes);
  }

  @Test
  @DisplayName("Should map products using ProductMapper")
  void shouldMapProductsUsingProductMapper() {
    service.save(aggregate);

    verify(productMapper).toProductList(any());
  }

  @Test
  @DisplayName("Should throw BookingProjectionException when save fails")
  void shouldThrowBookingProjectionExceptionWhenSaveFails() {
    when(bookingProjectionRepository.save(any())).thenThrow(new RuntimeException("Database error"));

    assertThrows(BookingProjectionException.class, () -> service.save(aggregate));
  }

  @Test
  @DisplayName("Should include metadata in saved entity")
  void shouldIncludeMetadataInSavedEntity() {
    service.save(aggregate);

    verify(bookingProjectionRepository).save(argThat(entity ->
      entity.getMetadata() != null &&
      entity.getMetadata().containsKey("source") &&
      "test".equals(entity.getMetadata().get("source"))
    ));
  }

  @Test
  @DisplayName("Should save entity with correct booking reference")
  void shouldSaveEntityWithCorrectBookingReference() {
    service.save(aggregate);

    verify(bookingProjectionRepository).save(argThat(entity ->
      "BK-12345".equals(entity.getBookingReference())
    ));
  }

  @Test
  @DisplayName("Should save entity with correct status")
  void shouldSaveEntityWithCorrectStatus() {
    service.save(aggregate);

    verify(bookingProjectionRepository).save(argThat(entity ->
      BookingStatus.CREATED.equals(entity.getStatus())
    ));
  }

  @Test
  @DisplayName("Should save entity with correct lead pax ID")
  void shouldSaveEntityWithCorrectLeadPaxId() {
    service.save(aggregate);

    verify(bookingProjectionRepository).save(argThat(entity ->
      leadPaxId.equals(entity.getLeadPaxId())
    ));
  }
}
