package com.cjrequena.sample.command.handler.shared.common.serializer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ValueDeserializer;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Log4j2
public class OffsetDateTimeDeserializer extends ValueDeserializer<OffsetDateTime> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  @Override
  public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
    try {
      String dateAsString = jsonParser.getString();
      if (dateAsString == null) {
        throw DatabindException.from(jsonParser, "OffsetDateTime argument is null.");
      }
      return OffsetDateTime.parse(dateAsString, FORMATTER);
    } catch (Exception ex) {
      log.warn("{}", ex.getMessage() + " - Invalid Format");
      throw ex;
    }
  }
}
