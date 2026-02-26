package com.cjrequena.sample.command.handler.domain.model.event;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.enums.EventType;
import com.cjrequena.sample.command.handler.domain.model.vo.BookingConfirmedEventDataVO;
import com.cjrequena.sample.command.handler.shared.common.util.ApplicationContextProvider;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;

/**
 * Event representing that a booking has been confirmed in the system.
 * <p>
 * This event follows the CloudEvents specification pattern, separating
 * event metadata (inherited from Event base class) from the event data payload
 * (encapsulated in BookingConfirmedEventDataVO).
 * </p>
 *
 * @author cjrequena
 */
@Getter
@SuperBuilder
@Jacksonized
@ToString(callSuper = true)
@Log4j2
public class BookingConfirmedEvent extends Event {

  private static EventMapper mapper = ApplicationContextProvider.getContext().getBean(EventMapper.class);

  /**
   * The event data payload containing booking confirmation information.
   */
  @NotNull(message = "Event data is required")
  @Valid
  private final BookingConfirmedEventDataVO data;

  /**
   * Returns the event type identifier.
   *
   * @return the event type as a string
   */
  @Nonnull
  @Override
  public String getEventType() {
    return EventType.BOOKING_CONFIRMED_EVENT.getType();
  }

  /**
   * Maps this domain event to its persistence entity representation.
   *
   * @return the EventEntity ready for persistence
   */
  @Override
  public EventEntity mapToEventEntity() {
    if (mapper == null) {
      throw new IllegalStateException(
        "EventMapper not initialized. Ensure Spring context is loaded."
      );
    }
    return mapper.bookingConfirmedEventToEventEntity(this);
  }
}
