package com.cjrequena.sample.query.handler.service.event;

import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import com.cjrequena.sample.es.core.service.EventStoreService;
import com.cjrequena.sample.query.handler.domain.exception.BookingProjectionException;
import com.cjrequena.sample.query.handler.domain.mapper.EventMapper;
import com.mongodb.MongoTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Query BookingEventHandler failure handling")
class BookingEventHandlerTest {

  private final UUID good = UUID.randomUUID();
  private final UUID bad = UUID.randomUUID();
  private final List<EventEntity> entities = List.of();

  private DeadLetterService deadLetterService;
  private BookingEventHandler handler;

  @BeforeEach
  void setUp() {
    EventMapper eventMapper = mock(EventMapper.class);
    List<Event> events = List.of(event(good), event(bad), event(good));
    when(eventMapper.toEventList(entities)).thenReturn(events);
    deadLetterService = mock(DeadLetterService.class);
    handler = spy(new BookingEventHandler(
      mock(EventStoreService.class), eventMapper, mock(EventStoreConfigurationProperties.class), List.of(), deadLetterService));
    ReflectionTestUtils.setField(handler, "projectionsHandlersEnabled", true);
    doNothing().when(handler).reproject(good);
  }

  @Test
  void permanentFailureIsDeadLetteredAndTheBatchCompletes() {
    doThrow(new BookingProjectionException("save failed", new DuplicateKeyException("dup")))
      .when(handler).reproject(bad);

    handler.handle(entities);

    verify(deadLetterService).record(eq(bad), eq("Booking"), anyString());
    verify(deadLetterService, never()).record(eq(good), any(), any());
    verify(handler).reproject(good);
  }

  @Test
  void transientFailureIsRethrownSoTheBatchIsRetried() {
    doThrow(new BookingProjectionException("save failed", new DataAccessResourceFailureException("mongo down")))
      .when(handler).reproject(bad);

    assertThatThrownBy(() -> handler.handle(entities)).isInstanceOf(BookingProjectionException.class);
    verify(deadLetterService, never()).record(any(), any(), any());
  }

  @Test
  void classifiesTransientCausesAnywhereInTheChain() {
    assertThat(BookingEventHandler.isTransient(new RuntimeException(new MongoTimeoutException("timeout")))).isTrue();
    assertThat(BookingEventHandler.isTransient(new IllegalStateException("bad data"))).isFalse();
    assertThat(BookingEventHandler.isTransient(new DuplicateKeyException("dup"))).isFalse();
  }

  @Test
  void doesNothingWhenProjectionsAreDisabled() {
    ReflectionTestUtils.setField(handler, "projectionsHandlersEnabled", false);
    handler.handle(entities);
    verify(handler, never()).reproject(any());
  }

  private static Event event(UUID aggregateId) {
    Event event = mock(Event.class);
    when(event.getAggregateId()).thenReturn(aggregateId);
    return event;
  }
}
