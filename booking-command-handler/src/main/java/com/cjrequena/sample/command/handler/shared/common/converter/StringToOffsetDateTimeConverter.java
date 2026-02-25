package com.cjrequena.sample.command.handler.shared.common.converter;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.time.OffsetDateTime;

@ReadingConverter
public class StringToOffsetDateTimeConverter implements Converter<String, OffsetDateTime> {

  @Override
  public OffsetDateTime convert(@NotNull @Nonnull String source) {
    return OffsetDateTime.parse(source);
  }
}
