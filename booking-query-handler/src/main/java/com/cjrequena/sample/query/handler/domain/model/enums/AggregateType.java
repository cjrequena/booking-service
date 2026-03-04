package com.cjrequena.sample.query.handler.domain.model.enums;

import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.query.handler.domain.model.aggregate.Booking;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

//@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum AggregateType {

  BOOKING_ORDER(Booking.class, Booking.class.getSimpleName());

  private final Class<? extends Aggregate> clazz;
  private final String type;

}
