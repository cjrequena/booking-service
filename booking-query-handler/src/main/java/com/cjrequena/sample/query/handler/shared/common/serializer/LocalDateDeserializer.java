package com.cjrequena.sample.query.handler.shared.common.serializer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Log4j2
public class LocalDateDeserializer extends ValueDeserializer<LocalDate> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  @Override
  public LocalDate deserialize(JsonParser jsonParser, DeserializationContext context) {
    try {
      String dateAsString = jsonParser.getString();
      if (dateAsString == null) {
        throw DatabindException.from(jsonParser, "LocalDate argument is null.");
      }
      return LocalDate.parse(dateAsString, FORMATTER);
    } catch (Exception ex) {
      log.warn("{}", ex.getMessage() + " - Invalid Format");
      throw ex;
    }
  }
}
