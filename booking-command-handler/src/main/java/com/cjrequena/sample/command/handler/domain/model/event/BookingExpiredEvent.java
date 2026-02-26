package com.cjrequena.sample.command.handler.domain.model.event;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.enums.EventType;
import com.cjrequena.sample.command.handler.domain.model.vo.BookingExpiredEventDataVO;
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
 * Event representing that a booking has expired in the system.
 *
 * @author cjrequena
 */
@Getter
@SuperBuilder
@Jacksonized
@ToString(callSuper = true)
@Log4j2
public class BookingExpiredEvent extends Event {

  private static EventMapper mapper = ApplicationContextProvider.getContext().getBean(EventMapper.class);

  @NotNull(message = "Event data is required")
  @Valid
  private final BookingExpiredEventDataVO data;

  @Nonnull
  @Override
  public String getEventType() {
    return EventType.BOOKING_EXPIRED_EVENT.getType();
  }

  @Override
  public EventEntity mapToEventEntity() {
    if (mapper == null) {
      throw new IllegalStateException(
        "EventMapper not initialized. Ensure Spring context is loaded."
      );
    }
    return mapper.bookingExpiredEventToEventEntity(this);
  }
}
