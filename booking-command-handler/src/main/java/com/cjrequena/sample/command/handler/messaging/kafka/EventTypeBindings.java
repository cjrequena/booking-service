package com.cjrequena.sample.command.handler.messaging.kafka;

import com.cjrequena.sample.command.handler.domain.exception.InvalidArgumentException;
import com.cjrequena.sample.command.handler.domain.model.enums.EventType;

import java.util.Map;

import static com.cjrequena.sample.command.handler.shared.common.Constant.KAFKA_BOOKING_ORDER_OUTBOUND_CHANNEL;

/**
 * Routes each domain event type to its outbound Spring Cloud Stream binding.
 *
 * <p>Adding an event type without a binding fails loudly at publish time rather than silently
 * dropping the event.</p>
 */
public final class EventTypeBindings {

  public static final String BOOKING_EVENTS_OUT = KAFKA_BOOKING_ORDER_OUTBOUND_CHANNEL;

  private static final Map<String, String> BINDINGS = Map.ofEntries(
    Map.entry(EventType.BOOKING_CREATED_EVENT.getType(), BOOKING_EVENTS_OUT),
    Map.entry(EventType.BOOKING_CONFIRMED_EVENT.getType(), BOOKING_EVENTS_OUT),
    Map.entry(EventType.BOOKING_CANCELLED_EVENT.getType(), BOOKING_EVENTS_OUT),
    Map.entry(EventType.BOOKING_COMPLETED_EVENT.getType(), BOOKING_EVENTS_OUT),
    Map.entry(EventType.BOOKING_EXPIRED_EVENT.getType(), BOOKING_EVENTS_OUT)
  );

  private EventTypeBindings() {
  }

  public static String bindingFor(String eventType) {
    String binding = BINDINGS.get(eventType);
    if (binding == null) {
      throw new InvalidArgumentException("No Kafka binding configured for event type '%s'".formatted(eventType));
    }
    return binding;
  }
}
