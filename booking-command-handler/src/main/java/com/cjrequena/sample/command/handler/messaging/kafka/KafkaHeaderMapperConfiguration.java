package com.cjrequena.sample.command.handler.messaging.kafka;

import org.springframework.cloud.stream.binder.kafka.common.BinderHeaderMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaderMapper;

/**
 * Binder-level Kafka header mapper keeping the wire headers CloudEvents-clean.
 *
 * <p>With the default mapper every String header is recorded in a
 * {@code spring_json_header_types} record header, and Spring Cloud Function adds a non-spec
 * {@code message-type: cloudevent} header to outbound cloud events. This mapper allowlists the
 * CloudEvents attributes ({@code ce_*} plus {@code content-type}, which carries
 * {@code datacontenttype} in binary mode), extensions and trace-context headers for outbound records and writes
 * them as raw UTF-8 strings ({@code mapAllStringsOut}), so neither framework header reaches the
 * wire. The patterns apply to outbound mapping only; all inbound headers are still mapped.</p>
 *
 * <p>Registered via {@code spring.cloud.stream.kafka.binder.header-mapper-bean-name}.</p>
 */
@Configuration
public class KafkaHeaderMapperConfiguration {

  @Bean("kafkaBinderHeaderMapper")
  public KafkaHeaderMapper kafkaBinderHeaderMapper() {
    BinderHeaderMapper mapper = new BinderHeaderMapper(
      "ce_*",
      "content-type",
      "correlationid",
      "traceparent",
      "tracestate",
      "b3"
    );
    mapper.setMapAllStringsOut(true);
    return mapper;
  }
}
