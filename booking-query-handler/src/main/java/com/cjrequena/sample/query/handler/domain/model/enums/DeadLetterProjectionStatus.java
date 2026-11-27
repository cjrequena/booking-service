package com.cjrequena.sample.query.handler.domain.model.enums;

/** Lifecycle of a dead-lettered projection. */
public enum DeadLetterProjectionStatus {
  /** Recorded, not yet replayed. */
  PENDING,
  /** Re-projected successfully; the aggregate is back in the read model. */
  REPLAYED,
  /** The last replay failed again; see {@code lastError}. Can be replayed again. */
  FAILED,
  /** Closed by an operator without replaying. */
  DISCARDED
}
