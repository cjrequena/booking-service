package com.cjrequena.sample.command.handler.service.projection;

import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import jakarta.annotation.Nonnull;

public interface ProjectionHandler {

  void handle(Aggregate aggregate);

  @Nonnull
  AggregateType getAggregateType();
}
