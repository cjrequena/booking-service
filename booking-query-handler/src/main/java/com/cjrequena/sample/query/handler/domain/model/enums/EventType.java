package com.cjrequena.sample.query.handler.domain.model.enums;


import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.query.handler.domain.exception.InvalidArgumentException;
import com.cjrequena.sample.query.handler.domain.model.event.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum EventType {

  BOOKING_CREATED_EVENT(BookingCreatedEvent.class, BookingCreatedEvent.class.getSimpleName()),
  BOOKING_CONFIRMED_EVENT(BookingConfirmedEvent.class, BookingConfirmedEvent.class.getSimpleName()),
  BOOKING_CANCELLED_EVENT(BookingCancelledEvent.class, BookingCancelledEvent.class.getSimpleName()),
  BOOKING_COMPLETED_EVENT(BookingCompletedEvent.class, BookingCompletedEvent.class.getSimpleName()),
  BOOKING_EXPIRED_EVENT(BookingExpiredEvent.class, BookingExpiredEvent.class.getSimpleName());
  // ==
  private final Class<? extends Event> clazz;
  private final String type;

  @JsonCreator
  public static EventType from(String type) {
    for (EventType eventType : EventType.values()) {
      if (eventType.type.equals(type)) {
        return eventType;
      }
    }
    throw new InvalidArgumentException("Unexpected type '" + type + "'");
  }

  @JsonValue
  @Override
  public String toString() {
    return String.valueOf(type);
  }
}
