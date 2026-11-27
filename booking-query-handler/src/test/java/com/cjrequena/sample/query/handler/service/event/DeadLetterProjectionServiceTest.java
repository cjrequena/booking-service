package com.cjrequena.sample.query.handler.service.event;

import com.cjrequena.sample.query.handler.domain.exception.DeadLetterProjectionNotFoundException;
import com.cjrequena.sample.query.handler.domain.exception.InvalidDeadLetterProjectionStatusException;
import com.cjrequena.sample.query.handler.domain.model.PageResult;
import com.cjrequena.sample.query.handler.domain.model.enums.DeadLetterProjectionStatus;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.DeadLetterProjectionEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.repository.DeadLetterProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeadLetterProjectionServiceTest {

  private final UUID id = UUID.randomUUID();
  private final UUID aggregateId = UUID.randomUUID();

  private DeadLetterProjectionRepository repository;
  private BookingEventHandler bookingEventHandler;
  private DeadLetterProjectionService service;

  @BeforeEach
  void setUp() {
    repository = mock(DeadLetterProjectionRepository.class);
    bookingEventHandler = mock(BookingEventHandler.class);
    service = new DeadLetterProjectionService(repository, bookingEventHandler);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private DeadLetterProjectionEntity stored(DeadLetterProjectionStatus status) {
    DeadLetterProjectionEntity entity = DeadLetterProjectionEntity.builder()
      .id(id).aggregateId(aggregateId).aggregateType("Booking").errorMessage("boom").status(status).attempts(0).build();
    when(repository.findById(id)).thenReturn(Optional.of(entity));
    return entity;
  }

  @Test
  void successfulReplayMarksReplayed() {
    stored(DeadLetterProjectionStatus.PENDING);

    DeadLetterProjectionEntity result = service.replay(id);

    verify(bookingEventHandler).reproject(aggregateId);
    assertThat(result.getStatus()).isEqualTo(DeadLetterProjectionStatus.REPLAYED);
    assertThat(result.getAttempts()).isEqualTo(1);
    assertThat(result.getLastAttemptAt()).isNotNull();
    assertThat(result.getLastError()).isNull();
  }

  @Test
  void failedReplayIsRecordedNotThrown() {
    stored(DeadLetterProjectionStatus.FAILED);
    doThrow(new IllegalStateException("still broken")).when(bookingEventHandler).reproject(aggregateId);

    DeadLetterProjectionEntity result = service.replay(id);

    assertThat(result.getStatus()).isEqualTo(DeadLetterProjectionStatus.FAILED);
    assertThat(result.getLastError()).isEqualTo("still broken");
    assertThat(result.getAttempts()).isEqualTo(1);
  }

  @Test
  void closedEntriesCannotBeReplayedOrDiscarded() {
    stored(DeadLetterProjectionStatus.REPLAYED);
    assertThatThrownBy(() -> service.replay(id)).isInstanceOf(InvalidDeadLetterProjectionStatusException.class);
    assertThatThrownBy(() -> service.discard(id)).isInstanceOf(InvalidDeadLetterProjectionStatusException.class);
    verify(bookingEventHandler, never()).reproject(any());
  }

  @Test
  void discardClosesAPendingEntry() {
    stored(DeadLetterProjectionStatus.PENDING);
    assertThat(service.discard(id).getStatus()).isEqualTo(DeadLetterProjectionStatus.DISCARDED);
  }

  @Test
  void unknownIdIsNotFound() {
    when(repository.findById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.retrieveById(id)).isInstanceOf(DeadLetterProjectionNotFoundException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void retrievePagesByOffset() {
    DeadLetterProjectionEntity entity = stored(DeadLetterProjectionStatus.PENDING);
    when(repository.findAll(any(Example.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));

    PageResult<DeadLetterProjectionEntity> page =
      service.retrieve(DeadLetterProjectionStatus.PENDING, null, 0, 20, Sort.by("createdAt"));

    assertThat(page.content()).containsExactly(entity);
    assertThat(page.totalElements()).isEqualTo(1);
    assertThat(page.limit()).isEqualTo(20);
  }
}
