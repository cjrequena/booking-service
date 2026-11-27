package com.cjrequena.sample.command.handler.messaging.kafka;

import com.cjrequena.sample.es.core.domain.model.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Forwards domain events to Kafka as CloudEvents (binary mode: payload is the event data, the
 * attributes travel as {@code ce_*} headers, the record key is the aggregate id).
 *
 * <p>Delivery is at-least-once. Events reach this listener from the event store subscription
 * poller, whose transaction also advances the subscription offset. The listener runs synchronously
 * inside that transaction and the producer binding is {@code sync: true}, so a failed send throws,
 * the transaction rolls back, the offset stays put and the batch is re-delivered on the next poll.
 * Consumers must therefore de-duplicate on {@code ce_id}.</p>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class KafkaEventEmitter {

  static final String CE_SOURCE = "/booking-command-handler";
  static final String CONTENT_TYPE_HEADER = "content-type";

  private final StreamBridge streamBridge;

  @EventListener
  public void onEvent(Event event) {
    String binding = EventTypeBindings.bindingFor(event.getEventType());

    Map<String, Object> headers = new HashMap<>();
    headers.put(KafkaHeaders.KEY, event.getAggregateId().toString());
    headers.put("ce_specversion", "1.0");
    headers.put("ce_id", event.getEventId().toString());
    headers.put("ce_type", event.getEventType());
    headers.put("ce_source", CE_SOURCE);
    headers.put("ce_time", event.getTime().toString());
    // CloudEvents Kafka binding, binary mode: datacontenttype travels as the Kafka "content-type"
    // header, not as ce_datacontenttype (Spring would also rewrite that one into its own contentType).
    headers.put(CONTENT_TYPE_HEADER, MimeTypeUtils.APPLICATION_JSON_VALUE);
    Map<String, Object> extensions = event.getExtension();
    if (extensions != null) {
      extensions.forEach((key, value) -> {
        if (value != null) {
          headers.put(key, value.toString());
        }
      });
    }

    Message<Object> message = MessageBuilder
      .withPayload(event.getData())
      .copyHeaders(headers)
      .build();

    if (!streamBridge.send(binding, message)) {
      throw new IllegalStateException("Kafka binding '%s' refused event %s".formatted(binding, event.getEventId()));
    }
    log.info("Event forwarded to Kafka [id={}, type={}, aggregateId={}, binding={}]",
      event.getEventId(), event.getEventType(), event.getAggregateId(), binding);
  }
}
