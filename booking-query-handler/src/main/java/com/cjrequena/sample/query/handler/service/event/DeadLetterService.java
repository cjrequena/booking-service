package com.cjrequena.sample.query.handler.service.event;

import com.cjrequena.sample.query.handler.domain.model.enums.DeadLetterProjectionStatus;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.DeadLetterProjectionEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.repository.DeadLetterProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** Records projections that failed permanently. */
@Service
@RequiredArgsConstructor
@Log4j2
public class DeadLetterService {

  private final DeadLetterProjectionRepository deadLetterProjectionRepository;

  public void record(UUID aggregateId, String aggregateType, String errorMessage) {
    Instant now = Instant.now();
    deadLetterProjectionRepository.save(
      DeadLetterProjectionEntity.builder()
        .id(UUID.randomUUID())
        .aggregateId(aggregateId)
        .aggregateType(aggregateType)
        .errorMessage(errorMessage == null ? "unknown" : errorMessage)
        .status(DeadLetterProjectionStatus.PENDING)
        .attempts(0)
        .createdAt(now)
        .updatedAt(now)
        .build()
    );
  }
}
