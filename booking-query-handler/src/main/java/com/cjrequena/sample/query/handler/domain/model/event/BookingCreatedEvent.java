package com.cjrequena.sample.query.handler.domain.model.event;

import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import com.cjrequena.sample.query.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.query.handler.domain.model.enums.EventType;
import com.cjrequena.sample.query.handler.domain.model.vo.BookingCreatedEventDataVO;
import com.cjrequena.sample.query.handler.shared.common.util.ApplicationContextProvider;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;

/**
 * Event representing that a booking has been created in the system.
 * <p>
 * This event follows the CloudEvents specification pattern, separating
 * event metadata (inherited from Event base class) from the event data payload
 * (encapsulated in BookingCreatedEventData).
 * </p>
 * <p>
 * Event Structure:
 * <ul>
 *   <li>Metadata: eventId, aggregateId, aggregateVersion, eventType, time</li>
 *   <li>Data: booking details (bookingId, reference, status, paxes, products)</li>
 * </ul>
 * </p>
 * <p>
 * This separation provides:
 * <ul>
 *   <li>Clear distinction between envelope and payload</li>
 *   <li>Better alignment with CloudEvents specification</li>
 *   <li>Easier event versioning and evolution</li>
 *   <li>Simplified serialization/deserialization</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Getter
@SuperBuilder
@Jacksonized
@ToString(callSuper = true)
@Log4j2
public class BookingCreatedEvent extends Event {

  private static EventMapper mapper  = ApplicationContextProvider.getContext().getBean(EventMapper.class);

  /**
   * The event data payload containing all booking information.
   * <p>
   * This follows the CloudEvents pattern where the actual business data
   * is separated from the event metadata.
   * </p>
   */
  @NotNull(message = "Event data is required")
  @Valid
  private final BookingCreatedEventDataVO data;

  /**
   * Returns the event type identifier.
   *
   * @return the event type as a string
   */
  @Nonnull
  @Override
  public String getEventType() {
    return EventType.BOOKING_CREATED_EVENT.getType();
  }

  /**
   * Maps this domain event to its persistence entity representation.
   * <p>
   * Uses MapStruct-generated mapper to convert the event to an EventEntity
   * with JSON serialization and all necessary metadata.
   * </p>
   *
   * @return the EventEntity ready for persistence
   */
  @Override
  public EventEntity mapToEventEntity() {
    if (mapper == null) {
      throw new IllegalStateException(
        "BookingCreatedEventMapper not initialized. Ensure Spring context is loaded."
      );
    }
    return mapper.bookingCreatedEventToEventEntity(this);
  }
}
