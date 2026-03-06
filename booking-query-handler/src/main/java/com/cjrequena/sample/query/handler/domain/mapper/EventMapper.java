package com.cjrequena.sample.query.handler.domain.mapper;

import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import com.cjrequena.sample.query.handler.domain.exception.MapperException;
import com.cjrequena.sample.query.handler.domain.model.event.*;
import com.cjrequena.sample.query.handler.domain.model.vo.*;
import com.cjrequena.sample.query.handler.shared.common.util.JsonUtil;
import org.mapstruct.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(
  componentModel = "spring",
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  builder = @Builder(disableBuilder = false)
)
public interface EventMapper {
  Logger log = LoggerFactory.getLogger(EventMapper.class);

  // ================================================================
  // Event Domains  <-->  Event Entities
  // ================================================================

  // ================================================================
  // BookingCreatedEvent
  // ================================================================
  @Mapping(target = "dataContentType", constant = "application/json")
  @Mapping(target = "data", ignore = true)
  @Mapping(target = "eventId", ignore = true)
  @Mapping(target = "extension", ignore = true)
  @Mapping(target = "offsetId", ignore = true)
  @Mapping(target = "offsetTxId", ignore = true)
  @Mapping(target = "time", ignore = true)
  EventEntity bookingCreatedEventToEventEntity(BookingCreatedEvent event);

  /** Serializes {@link BookingCreatedEvent#getData()} to JSON and sets it on the entity. */
  @AfterMapping
  default void populateEntityFields(BookingCreatedEvent event, @MappingTarget EventEntity entity) {
    if (event == null) {
      return;
    }
    try {
      entity.setData(JsonUtil.objectToJsonString(event.getData()));
    } catch (Exception e) {
      throw new MapperException("Failed to serialize BookingCreatedEvent to JSON", e);
    }
    try {
      entity.setExtension(MetadataVO.of(event.getExtension()).toJson());
    } catch (Exception e) {
      throw new MapperException("Failed to serialize EventExtension to JSON", e);
    }
  }

  @Mapping(target = "data", expression = "java(deserializeDataToBookingCreatedEventDataVO(entity.getData()))")
  @Mapping(target = "extension", expression = "java(deserializeEventExtension(entity.getExtension()))")
  BookingCreatedEvent eventEntityToBookingCreatedEvent(EventEntity entity);

  /**
   * Deserializes the raw JSON string stored in {@link EventEntity#getData()} into a typed
   * {@link BookingCreatedEventDataVO}.
   *
   * @param json the JSON string to deserialize
   * @return the deserialized data value object
   */
  default BookingCreatedEventDataVO deserializeDataToBookingCreatedEventDataVO(String json) {
    try {
      return JsonUtil.jsonStringToObject(json, BookingCreatedEventDataVO.class);
    } catch (Exception e) {
      throw new MapperException("Failed to deserialize EventEntity data to BookingCreatedEventDataVO", e);
    }
  }

  // ================================================================
  // BookingConfirmedEvent
  // ================================================================
  @Mapping(target = "dataContentType", constant = "application/json")
  @Mapping(target = "data", ignore = true)
  @Mapping(target = "eventId", ignore = true)
  @Mapping(target = "extension", ignore = true)
  @Mapping(target = "offsetId", ignore = true)
  @Mapping(target = "offsetTxId", ignore = true)
  @Mapping(target = "time", ignore = true)
  EventEntity bookingConfirmedEventToEventEntity(BookingConfirmedEvent event);

  /** Serializes {@link BookingConfirmedEvent#getData()} to JSON and sets it on the entity. */
  @AfterMapping
  default void populateEntityFields(BookingConfirmedEvent event, @MappingTarget EventEntity entity) {
    if (event == null) {
      return;
    }
    try {
      entity.setData(JsonUtil.objectToJsonString(event.getData()));
    } catch (Exception e) {
      throw new MapperException("Failed to serialize BookingPlacedEvent to JSON", e);
    }
    try {
      entity.setExtension(MetadataVO.of(event.getExtension()).toJson());
    } catch (Exception e) {
      throw new MapperException("Failed to serialize EventExtension to JSON", e);
    }
  }

  @Mapping(target = "data", expression = "java(deserializeDataToBookingConfirmedEventDataVO(entity.getData()))")
  @Mapping(target = "extension", expression = "java(deserializeEventExtension(entity.getExtension()))")
  BookingConfirmedEvent eventEntityToBookingConfirmedEvent(EventEntity entity);


  /**
   * Deserializes the raw JSON string stored in {@link EventEntity#getData()} into a typed
   * {@link BookingConfirmedEventDataVO}.
   *
   * @param json the JSON string to deserialize
   * @return the deserialized data value object
   */
  default BookingConfirmedEventDataVO deserializeDataToBookingConfirmedEventDataVO(String json) {
    try {
      return JsonUtil.jsonStringToObject(json, BookingConfirmedEventDataVO.class);
    } catch (Exception e) {
      throw new MapperException("Failed to deserialize EventEntity data to BookingConfirmedEventDataVO", e);
    }
  }

  // ================================================================
  // BookingCancelledEvent
  // ================================================================
  @Mapping(target = "dataContentType", constant = "application/json")
  @Mapping(target = "data", ignore = true)
  @Mapping(target = "eventId", ignore = true)
  @Mapping(target = "extension", ignore = true)
  @Mapping(target = "offsetId", ignore = true)
  @Mapping(target = "offsetTxId", ignore = true)
  @Mapping(target = "time", ignore = true)
  EventEntity bookingCancelledEventToEventEntity(BookingCancelledEvent event);

  /** Serializes {@link BookingCancelledEvent#getData()} to JSON and sets it on the entity. */
  @AfterMapping
  default void populateEntityFields(BookingCancelledEvent event, @MappingTarget EventEntity entity) {
    if (event == null) {
      return;
    }
    try {
      entity.setData(JsonUtil.objectToJsonString(event.getData()));
    } catch (Exception e) {
      throw new MapperException("Failed to serialize BookingPlacedEvent to JSON", e);
    }
    try {
      entity.setExtension(MetadataVO.of(event.getExtension()).toJson());
    } catch (Exception e) {
      throw new MapperException("Failed to serialize EventExtension to JSON", e);
    }
  }

  @Mapping(target = "data", expression = "java(deserializeDataToBookingCancelledEventDataVO(entity.getData()))")
  @Mapping(target = "extension", expression = "java(deserializeEventExtension(entity.getExtension()))")
  BookingCancelledEvent eventEntityToBookingCancelledEvent(EventEntity entity);

  /**
   * Deserializes the raw JSON string stored in {@link EventEntity#getData()} into a typed
   * {@link BookingCancelledEventDataVO}.
   *
   * @param json the JSON string to deserialize
   * @return the deserialized data value object
   */
  default BookingCancelledEventDataVO deserializeDataToBookingCancelledEventDataVO(String json) {
    try {
      return JsonUtil.jsonStringToObject(json, BookingCancelledEventDataVO.class);
    } catch (Exception e) {
      throw new MapperException("Failed to deserialize EventEntity data to BookingCancelledEventDataVO", e);
    }
  }

  // ================================================================
  // BookingCompletedEvent
  // ================================================================
  @Mapping(target = "dataContentType", constant = "application/json")
  @Mapping(target = "data", ignore = true)
  @Mapping(target = "eventId", ignore = true)
  @Mapping(target = "extension", ignore = true)
  @Mapping(target = "offsetId", ignore = true)
  @Mapping(target = "offsetTxId", ignore = true)
  @Mapping(target = "time", ignore = true)
  EventEntity bookingCompletedEventToEventEntity(BookingCompletedEvent event);

  /** Serializes {@link BookingCompletedEvent#getData()} to JSON and sets it on the entity. */
  @AfterMapping
  default void populateEntityFields(BookingCompletedEvent event, @MappingTarget EventEntity entity) {
    if (event == null) {
      return;
    }
    try {
      entity.setData(JsonUtil.objectToJsonString(event.getData()));
    } catch (Exception e) {
      throw new MapperException("Failed to serialize BookingPlacedEvent to JSON", e);
    }
    try {
      entity.setExtension(MetadataVO.of(event.getExtension()).toJson());
    } catch (Exception e) {
      throw new MapperException("Failed to serialize EventExtension to JSON", e);
    }
  }

  @Mapping(target = "data", expression = "java(deserializeDataToBookingCompletedEventDataVO(entity.getData()))")
  @Mapping(target = "extension", expression = "java(deserializeEventExtension(entity.getExtension()))")
  BookingCompletedEvent eventEntityToBookingCompletedEvent(EventEntity entity);

  /**
   * Deserializes the raw JSON string stored in {@link EventEntity#getData()} into a typed
   * {@link BookingCompletedEventDataVO}.
   *
   * @param json the JSON string to deserialize
   * @return the deserialized data value object
   */
  default BookingCompletedEventDataVO deserializeDataToBookingCompletedEventDataVO(String json) {
    try {
      return JsonUtil.jsonStringToObject(json, BookingCompletedEventDataVO.class);
    } catch (Exception e) {
      throw new MapperException("Failed to deserialize EventEntity data to BookingCompletedEventDataVO", e);
    }
  }

  // ================================================================
  // BookingExpiredEvent
  // ================================================================
  @Mapping(target = "dataContentType", constant = "application/json")
  @Mapping(target = "data", ignore = true)
  @Mapping(target = "eventId", ignore = true)
  @Mapping(target = "extension", ignore = true)
  @Mapping(target = "offsetId", ignore = true)
  @Mapping(target = "offsetTxId", ignore = true)
  @Mapping(target = "time", ignore = true)
  EventEntity bookingExpiredEventToEventEntity(BookingExpiredEvent event);

  /** Serializes {@link BookingExpiredEvent#getData()} to JSON and sets it on the entity. */
  @AfterMapping
  default void populateEntityFields(BookingExpiredEvent event, @MappingTarget EventEntity entity) {
    if (event == null) {
      return;
    }
    try {
      entity.setData(JsonUtil.objectToJsonString(event.getData()));
    } catch (Exception e) {
      throw new MapperException("Failed to serialize BookingPlacedEvent to JSON", e);
    }
    try {
      entity.setExtension(MetadataVO.of(event.getExtension()).toJson());
    } catch (Exception e) {
      throw new MapperException("Failed to serialize EventExtension to JSON", e);
    }
  }

  @Mapping(target = "data", expression = "java(deserializeDataToBookingExpiredEventDataVO(entity.getData()))")
  @Mapping(target = "extension", expression = "java(deserializeEventExtension(entity.getExtension()))")
  BookingExpiredEvent eventEntityToBookingExpiredEvent(EventEntity entity);

  /**
   * Deserializes the raw JSON string stored in {@link EventEntity#getData()} into a typed
   * {@link BookingExpiredEventDataVO}.
   *
   * @param json the JSON string to deserialize
   * @return the deserialized data value object
   */
  default BookingExpiredEventDataVO deserializeDataToBookingExpiredEventDataVO(String json) {
    try {
      return JsonUtil.jsonStringToObject(json, BookingExpiredEventDataVO.class);
    } catch (Exception e) {
      throw new MapperException("Failed to deserialize EventEntity data to BookingExpiredEventDataVO", e);
    }
  }

  // ================================================================
  //
  // ================================================================
  default Map<String,Object> deserializeEventExtension(String json) {
    try {
      return MetadataVO.of(json).toMap();
    } catch (Exception e) {
      throw new MapperException("Failed to deserialize EventEntity data to BookingCreatedEventDataVO", e);
    }
  }

  // ================================================================
  // Method to map a List of EventEntity to a List of Event
  // ================================================================
  default List<Event> toEventList(List<EventEntity> eventEntities) {
    return eventEntities
      .stream()
      .map(this::toEvent)  // Call the helper method for individual mapping
      .collect(Collectors.toList());
  }
  // ================================================================
  // Helper method to map a single EventEntity to an Event (AccountCreatedEvent, AccountCreditedEvent, etc.)
  // ================================================================
  default Event toEvent(EventEntity eventEntity) {
    // Assuming EventEntity has a type field or some kind of discriminator
    switch (eventEntity.getEventType()) {
      case "BookingCreatedEvent":
        return eventEntityToBookingCreatedEvent(eventEntity);
      case "BookingConfirmedEvent":
        return eventEntityToBookingConfirmedEvent(eventEntity);
      case "BookingCancelledEvent":
        return eventEntityToBookingCancelledEvent(eventEntity);
      case "BookingCompletedEvent":
        return eventEntityToBookingCompletedEvent(eventEntity);
      case "BookingExpiredEvent":
        return eventEntityToBookingExpiredEvent(eventEntity);
      default:
        String errorMessage = String.format("Error mapping to event, unknown event type: %s", eventEntity.getEventType());
        log.error(errorMessage);
        throw new MapperException(errorMessage);
    }
  }

}
