package com.cjrequena.sample.query.handler.shared.common.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


public class LocalTimeSerializer extends ValueSerializer<LocalTime> {


  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

  /**
   *
   * @param value
   * @param generator
   * @param provider
   * @throws IOException
   */
  @Override
  public void serialize(LocalTime value, JsonGenerator generator, SerializationContext provider) {
    generator.writeString(value.format(FORMATTER));
  }
}
