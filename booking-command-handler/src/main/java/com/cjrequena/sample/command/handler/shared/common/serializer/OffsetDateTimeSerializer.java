package com.cjrequena.sample.command.handler.shared.common.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;


public class OffsetDateTimeSerializer extends ValueSerializer<OffsetDateTime> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  @Override
  public void serialize(OffsetDateTime value, JsonGenerator generator, SerializationContext provider) {
    generator.writeString(value.format(FORMATTER));
  }
}
