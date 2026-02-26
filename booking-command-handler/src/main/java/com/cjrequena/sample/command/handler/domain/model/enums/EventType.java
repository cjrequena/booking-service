package com.cjrequena.sample.command.handler.domain.model.enums;

import com.cjrequena.sample.command.handler.domain.exception.InvalidArgumentException;
import com.cjrequena.sample.command.handler.domain.model.event.BookingConfirmedEvent;
import com.cjrequena.sample.command.handler.domain.model.event.BookingCreatedEvent;
import com.cjrequena.sample.command.handler.domain.model.event.BookingPlacedEvent;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum EventType {

  BOOKING_CREATED_EVENT(BookingCreatedEvent.class, BookingCreatedEvent.class.getSimpleName()),
  BOOKING_PLACED_EVENT(BookingPlacedEvent.class, BookingPlacedEvent.class.getSimpleName()),
  BOOKING_CONFIRMED_EVENT(BookingConfirmedEvent.class, BookingConfirmedEvent.class.getSimpleName());
  // ==
//  BOOKING_ORDER_PLACED_EVENT(BookingOrderPlacedEvent.class, BookingOrderPlacedEvent.class.getName()),
//  BOOKING_ORDER_CANCELLED_EVENT(BookingOrderCancelledEvent.class, BookingOrderCancelledEvent.class.getName()),
//  PRODUCT_BOOKING_ORDER_CANCELLED_EVENT(BookingOrderProductCancelledEvent.class, BookingOrderProductCancelledEvent.class.getName()),
//  BOOKING_ORDER_COMPLETED_EVENT(BookingOrderCompletedEvent.class, BookingOrderCompletedEvent.class.getName()),
//  BOOKING_ORDER_ACCEPTED_EVENT(BookingOrderAcceptedEvent.class, BookingOrderAcceptedEvent.class.getName()),
//  BOOKING_ORDER_PAYMENT_INITIATED_EVENT(BookingOrderPaymentInitiatedEvent.class, BookingOrderPaymentInitiatedEvent.class.getName()),
//  BOOKING_ORDER_PAYMENT_CONFIRMED_EVENT(BookingOrderPaymentConfirmedEvent.class, BookingOrderPaymentConfirmedEvent.class.getName());


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
