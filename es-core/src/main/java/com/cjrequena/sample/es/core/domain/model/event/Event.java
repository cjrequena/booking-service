package com.cjrequena.sample.es.core.domain.model.event;

import com.cjrequena.sample.es.core.persistence.entity.AbstractEventEntity;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@SuperBuilder
@ToString
@Log4j2
public abstract class Event {

  // Unique id for the specific message. This id is globally unique
  protected UUID eventId;

  // The event offset_txid
  protected long offsetId;

  // The event offset_txid
  protected long offsetTxId;

  // Unique aggregateId for the specific message. This id is globally unique
  protected final UUID aggregateId;

  // The event aggregateVersion.
  protected final long aggregateVersion;

  // Type of message
  protected String eventType;

  // The content type of the event data. Must adhere to RFC 2046 format.
  protected String dataContentType;

  // Base64 encoded event payload. Must adhere to RFC4648.
  protected String dataBase64;

  // A URI describing the schema for the event data
  protected String dataSchema;

  // The time the event occurred
  protected OffsetDateTime time;

  // Metadata extensions
  protected Map<String, Object> extension;

  //
  public abstract Object getData();

  public AbstractEventEntity mapToEventEntity() {
    log.info("Mapping to event entity {}", this);
    return invoke();
  }

  @SneakyThrows
  private AbstractEventEntity invoke() {
    Class<?> parameterType = this.getClass();
    Method method = this.getClass().getMethod("mapToEventEntity");
    return (AbstractEventEntity) method.invoke(this);
  }

}
