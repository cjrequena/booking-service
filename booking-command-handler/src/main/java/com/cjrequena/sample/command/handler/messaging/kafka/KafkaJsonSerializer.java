package com.cjrequena.sample.command.handler.messaging.kafka;

import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The value serializer every producer binding names. Kafka instantiates it reflectively, so it never
 * sees the application's {@code JacksonConfiguration}, and Spring Kafka's own default mapper is plain
 * Jackson 3: alphabetical property order, enums written through {@code toString()}, and other changed
 * defaults would alter the payloads consumers were built against. This keeps the mapper Spring Kafka
 * used under Jackson 2 ({@code JacksonUtils.enhancedObjectMapper()}).
 */
public class KafkaJsonSerializer<T> extends JacksonJsonSerializer<T> {

  public KafkaJsonSerializer() {
    super(JsonMapper.builder()
      .configureForJackson2()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
      .build());
  }
}
