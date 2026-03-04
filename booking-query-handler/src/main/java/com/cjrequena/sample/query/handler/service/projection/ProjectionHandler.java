package com.cjrequena.sample.query.handler.service.projection;

import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.query.handler.domain.model.enums.AggregateType;
import jakarta.annotation.Nonnull;

public interface ProjectionHandler {

  void handle(Aggregate aggregate);

  @Nonnull
  AggregateType getAggregateType();
}
