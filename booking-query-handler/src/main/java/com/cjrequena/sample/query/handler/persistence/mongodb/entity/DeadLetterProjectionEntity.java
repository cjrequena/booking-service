package com.cjrequena.sample.query.handler.persistence.mongodb.entity;

import com.cjrequena.sample.query.handler.domain.model.enums.DeadLetterProjectionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

/**
 * Durable record of an aggregate whose projection failed permanently. Written instead of jamming
 * the event subscription on it or silently dropping it from the read model, so the failure is
 * visible and can be replayed once its cause is fixed.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Document(collection = "dead_letter_projection")
public class DeadLetterProjectionEntity {

  @Id
  private UUID id;
  @Indexed
  private UUID aggregateId;
  private String aggregateType;
  private String errorMessage;
  @Indexed
  private DeadLetterProjectionStatus status;
  private Integer attempts;
  private Instant lastAttemptAt;
  private String lastError;
  private Instant createdAt;
  private Instant updatedAt;
}
