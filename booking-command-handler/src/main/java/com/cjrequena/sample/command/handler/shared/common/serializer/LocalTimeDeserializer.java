package com.cjrequena.sample.command.handler.shared.common.serializer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Log4j2
public class LocalTimeDeserializer extends ValueDeserializer<LocalTime> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

  @Override
  public LocalTime deserialize(JsonParser  jsonParser, DeserializationContext context) {
    try {
      String dateAsString = jsonParser.getString();
      if (dateAsString == null) {
        throw DatabindException.from(jsonParser, "LocalTime argument is null.");
      }
      return LocalTime.parse(dateAsString, FORMATTER);
    } catch (Exception ex) {
      log.warn("{}", ex.getMessage() + " - Invalid Format");
      throw ex;
    }
  }
}
