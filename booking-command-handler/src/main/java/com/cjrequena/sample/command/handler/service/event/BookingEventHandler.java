package com.cjrequena.sample.command.handler.service.event;

import com.cjrequena.sample.command.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import com.cjrequena.sample.es.core.service.EventStoreService;
import jakarta.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cjrequena.sample.command.handler.shared.common.Constant.KAFKA_BOOKING_ORDER_OUTBOUND_CHANNEL;

@Component
@Transactional
@Log4j2
public class BookingEventHandler extends EventHandler {

  private final StreamBridge streamBridge;

  @Autowired
  public BookingEventHandler(
    EventStoreService eventStoreService,
    EventMapper eventMapper,
    EventStoreConfigurationProperties eventStoreConfigurationProperties,
    StreamBridge streamBridge
  ) {
    super(eventStoreService, eventMapper, eventStoreConfigurationProperties);
    this.streamBridge = streamBridge;
  }

  @Override
  public void handle(List<EventEntity> eventEntityList) {

    final List<Event> events = this.eventMapper.toEventList(eventEntityList);

    for (Event event : events) {
      if (log.isInfoEnabled()) {
        log.info("Handling event {} for aggregate {} with ID '{}' and aggregate version {}", event.getEventType(), getAggregateType(), event.getAggregateId(), event.getAggregateVersion());
      }
      // Here is to set the business logic to send the incoming event through an integration channel, e.g. Kafka, SNS, SQS, AWS Lambda, Webhook, etc.
      Map<String, String> headers = new HashMap<>();
      headers.put(KafkaHeaders.KEY, event.getAggregateId().toString());
      headers.put("ce_specversion","V1");
      headers.put("ce_id", event.getEventId().toString());
      headers.put("ce_type", event.getEventType());
      headers.put("ce_source", URI.create("/booking-service").toString());
      headers.put("ce_time",event.getTime().toString());
      headers.put("ce_datacontenttype", MimeTypeUtils.APPLICATION_JSON.toString());
      final Map<String, Object> extensions = event.getExtension();
      extensions.forEach((key, value) -> {
        try {
          if (value == null) {
            log.debug("Skipping null extension value for key: {}", key);
            return;
          }
          headers.put(key, value.toString());
        } catch (Exception ex) {
          log.warn("Failed to add extension {}: {}", key, ex.getMessage());
        }
      });

      Message<Object> message = MessageBuilder.withPayload(event.getData()).copyHeaders(headers).build();
      streamBridge.send(KAFKA_BOOKING_ORDER_OUTBOUND_CHANNEL, message);
    }

  }

  @Nonnull
  @Override
  public AggregateType getAggregateType() {
    return AggregateType.BOOKING_ORDER;
  }
}
