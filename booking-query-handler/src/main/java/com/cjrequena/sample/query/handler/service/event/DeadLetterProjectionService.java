package com.cjrequena.sample.query.handler.service.event;

import com.cjrequena.sample.query.handler.domain.exception.DeadLetterProjectionNotFoundException;
import com.cjrequena.sample.query.handler.domain.exception.InvalidDeadLetterProjectionStatusException;
import com.cjrequena.sample.query.handler.domain.model.PageResult;
import com.cjrequena.sample.query.handler.domain.model.enums.DeadLetterProjectionStatus;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.DeadLetterProjectionEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.repository.DeadLetterProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Read, replay and discard for dead-lettered projections.
 *
 * <p>Replay re-projects the aggregate from the event store through
 * {@link BookingEventHandler#reproject(UUID)}, the same call live delivery makes. There is no
 * scheduled sweep: a permanent failure does not clear on its own, so replay is an operator action
 * taken once the cause is fixed.</p>
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class DeadLetterProjectionService {

  private final DeadLetterProjectionRepository deadLetterProjectionRepository;
  private final BookingEventHandler bookingEventHandler;

  public PageResult<DeadLetterProjectionEntity> retrieve(DeadLetterProjectionStatus status, String aggregateType,
                                                         int offset, int limit, Sort sort) {
    DeadLetterProjectionEntity probe = DeadLetterProjectionEntity.builder()
      .status(status)
      .aggregateType(aggregateType)
      .build();
    Page<DeadLetterProjectionEntity> page =
      deadLetterProjectionRepository.findAll(Example.of(probe), PageRequest.of(offset / limit, limit, sort));
    return PageResult.of(page.getContent(), page.getTotalElements(), offset / limit, limit);
  }

  public DeadLetterProjectionEntity retrieveById(UUID id) {
    return find(id);
  }

  /** Re-projects the aggregate and records the outcome, success or not. */
  public DeadLetterProjectionEntity replay(UUID id) {
    DeadLetterProjectionEntity entity = find(id);
    if (entity.getStatus() == DeadLetterProjectionStatus.REPLAYED || entity.getStatus() == DeadLetterProjectionStatus.DISCARDED) {
      throw new InvalidDeadLetterProjectionStatusException(
        "Dead-lettered projection %s is %s and cannot be replayed".formatted(id, entity.getStatus()));
    }
    Instant now = Instant.now();
    entity.setAttempts(entity.getAttempts() + 1);
    entity.setLastAttemptAt(now);
    entity.setUpdatedAt(now);
    try {
      bookingEventHandler.reproject(entity.getAggregateId());
      entity.setStatus(DeadLetterProjectionStatus.REPLAYED);
      entity.setLastError(null);
      log.info("Dead-lettered projection replayed [id={}, aggregateId={}]", id, entity.getAggregateId());
    } catch (RuntimeException ex) {
      entity.setStatus(DeadLetterProjectionStatus.FAILED);
      entity.setLastError(ex.getMessage());
      log.warn("Dead-lettered projection replay failed [id={}, aggregateId={}]: {}", id, entity.getAggregateId(), ex.getMessage());
    }
    return deadLetterProjectionRepository.save(entity);
  }

  public DeadLetterProjectionEntity discard(UUID id) {
    DeadLetterProjectionEntity entity = find(id);
    if (entity.getStatus() == DeadLetterProjectionStatus.REPLAYED || entity.getStatus() == DeadLetterProjectionStatus.DISCARDED) {
      throw new InvalidDeadLetterProjectionStatusException(
        "Dead-lettered projection %s is already %s".formatted(id, entity.getStatus()));
    }
    entity.setStatus(DeadLetterProjectionStatus.DISCARDED);
    entity.setUpdatedAt(Instant.now());
    log.info("Dead-lettered projection discarded [id={}, aggregateId={}]", id, entity.getAggregateId());
    return deadLetterProjectionRepository.save(entity);
  }

  private DeadLetterProjectionEntity find(UUID id) {
    return deadLetterProjectionRepository.findById(id)
      .orElseThrow(() -> new DeadLetterProjectionNotFoundException("Dead-lettered projection not found: " + id));
  }
}
