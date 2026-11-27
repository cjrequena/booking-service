package com.cjrequena.sample.query.handler.service.projection;

import com.cjrequena.sample.query.handler.domain.exception.BookingNotFoundException;
import com.cjrequena.sample.query.handler.domain.exception.BookingProjectionException;
import com.cjrequena.sample.query.handler.domain.mapper.PaxMapper;
import com.cjrequena.sample.query.handler.domain.mapper.ProductMapper;
import com.cjrequena.sample.query.handler.domain.model.PageResult;
import com.cjrequena.sample.query.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.query.handler.domain.model.enums.BookingStatus;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.BookingEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.repository.BookingProjectionRepository;
import com.cjrequena.sample.query.handler.service.sse.BookingUpdateMessage;
import com.cjrequena.sample.query.handler.service.sse.BookingUpdatePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingProjectionServiceTest {

  private BookingProjectionRepository repository;
  private BookingUpdatePublisher publisher;
  private MongoTemplate mongoTemplate;
  private BookingProjectionService service;

  @BeforeEach
  void setUp() {
    repository = mock(BookingProjectionRepository.class);
    publisher = mock(BookingUpdatePublisher.class);
    mongoTemplate = mock(MongoTemplate.class);
    service = new BookingProjectionService(repository, mock(PaxMapper.class), mock(ProductMapper.class), publisher, mongoTemplate);
  }

  @Test
  void saveStoresTheProjectionAndPublishesAnUpdate() {
    UUID bookingId = UUID.randomUUID();
    Booking booking = mock(Booking.class);
    when(booking.getBookingId()).thenReturn(bookingId);
    when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    BookingEntity saved = service.save(booking);

    assertThat(saved.getBookingId()).isEqualTo(bookingId);
    verify(publisher).publish(new BookingUpdateMessage(bookingId.toString(), "CONFIRMED"));
  }

  @Test
  void saveWrapsStoreFailuresAndPublishesNothing() {
    Booking booking = mock(Booking.class);
    when(booking.getBookingId()).thenReturn(UUID.randomUUID());
    when(repository.save(any())).thenThrow(new IllegalStateException("mongo said no"));

    assertThatThrownBy(() -> service.save(booking)).isInstanceOf(BookingProjectionException.class);
    verify(publisher, never()).publish(any());
  }

  @Test
  void saveRejectsAggregateWithoutId() {
    assertThatThrownBy(() -> service.save(mock(Booking.class))).isInstanceOf(BookingProjectionException.class);
  }

  @Test
  void retrieveByIdThrowsWhenMissing() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.retrieveById(id)).isInstanceOf(BookingNotFoundException.class);
  }

  @Test
  void retrieveAppliesFilterSortAndPaging() {
    BookingEntity entity = BookingEntity.builder().bookingId(UUID.randomUUID()).build();
    when(mongoTemplate.count(any(Query.class), eq(BookingEntity.class))).thenReturn(42L);
    when(mongoTemplate.find(any(Query.class), eq(BookingEntity.class))).thenReturn(List.of(entity));

    PageResult<BookingEntity> page = service.retrieve("status==CONFIRMED", 20, 10, "booking_reference,desc");

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).find(query.capture(), eq(BookingEntity.class));
    assertThat(query.getValue().getQueryObject().get("status")).isEqualTo(BookingStatus.CONFIRMED);
    assertThat(query.getValue().getSkip()).isEqualTo(20);
    assertThat(query.getValue().getLimit()).isEqualTo(10);
    assertThat(query.getValue().getSortObject().get("bookingReference")).isEqualTo(-1);
    assertThat(page.totalElements()).isEqualTo(42);
    assertThat(page.content()).containsExactly(entity);
  }

  @Test
  void retrieveWithoutPagingReturnsEverything() {
    when(mongoTemplate.find(any(Query.class), eq(BookingEntity.class))).thenReturn(List.of());
    PageResult<BookingEntity> page = service.retrieve(null, null, null, null);
    assertThat(page.offset()).isZero();
  }

  @Test
  void retrieveRejectsBadPaging() {
    assertThatThrownBy(() -> service.retrieve(null, -1, 10, null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.retrieve(null, 0, 500, null)).isInstanceOf(IllegalArgumentException.class);
  }
}
