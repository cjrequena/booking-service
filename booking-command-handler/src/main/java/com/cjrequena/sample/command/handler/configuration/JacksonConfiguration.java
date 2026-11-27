package com.cjrequena.sample.command.handler.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.text.SimpleDateFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

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

  public static JsonMapper buildObjectMapper() {
    return baseBuilder()
      .changeDefaultPropertyInclusion(incl -> JsonInclude.Value.construct(NON_EMPTY, NON_EMPTY))
      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
      .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
      .defaultDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
      .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
      .build();
  }

  /**
   * Jackson 3 changed a dozen defaults (alphabetical property order, enums via toString(), failing on
   * null primitives and trailing tokens, ...). {@code configureForJackson2()} restores the Jackson 2
   * behaviour every wire format, stored event and cache entry here was written with.
   */
  private static JsonMapper.Builder baseBuilder() {
    return JsonMapper.builder()
      .configureForJackson2()
      .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
      .filterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
  }

  @Primary
  @Bean(name = {"objectMapper"})
  public JsonMapper objectMapper() {
    return buildObjectMapper();
  }
}
