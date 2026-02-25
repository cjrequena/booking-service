package com.cjrequena.sample.query.handler.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * <p>
 * <p>
 * <p>
 * <p>
 *
 * @author cjrequena
 */
@Configuration
public class JacksonConfiguration {

  //  spring.jackson.serialization.FAIL_ON_EMPTY_BEANS=false
  //  spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS=false
  //  spring.jackson.deserialization.FAIL_ON_UNKNOWN_PROPERTIES=false
  //  spring.jackson.deserialization.ACCEPT_SINGLE_VALUE_AS_ARRAY=true
  //  spring.jackson.mapper.ACCEPT_CASE_INSENSITIVE_PROPERTIES=true
  //  spring.jackson.defaultPropertyInclusion=NON_NULL

  public static ObjectMapper buildObjectMapper() {
    final Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
    builder.indentOutput(false);
    builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    ObjectMapper objectMapper = builder
      .serializationInclusion(NON_NULL)
      .serializationInclusion(NON_EMPTY)
      .failOnEmptyBeans(false)
      .failOnUnknownProperties(false)
      .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
      .featuresToDisable(MapperFeature.DEFAULT_VIEW_INCLUSION, DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
      .simpleDateFormat("yyyy-MM-dd")
      .modules(new JavaTimeModule())
      .build();

    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    objectMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
    return objectMapper;
  }

  @Primary
  @Bean(name = {"objectMapper"})
  public ObjectMapper objectMapper() {
    return buildObjectMapper();
  }

  /**
   * Dedicated ObjectMapper for Redis serialization (uses default camelCase).
   * Does not use snake_case to match Java domain object field names.
   */
  @Bean(name = "redisObjectMapper")
  public ObjectMapper redisObjectMapper() {
    final Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
    builder.indentOutput(false);
    builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    ObjectMapper objectMapper = builder
      .serializationInclusion(NON_NULL)
      .failOnEmptyBeans(false)
      .failOnUnknownProperties(false)
      .featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
      .featuresToDisable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
      .modules(new JavaTimeModule())
      .build();

    // Don't set snake_case for Redis - keep default camelCase
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));

    // Enable polymorphic type handling for Redis
    objectMapper.activateDefaultTyping(
      objectMapper.getPolymorphicTypeValidator(),
      ObjectMapper.DefaultTyping.NON_FINAL,
      com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
    );

    return objectMapper;
  }

}
