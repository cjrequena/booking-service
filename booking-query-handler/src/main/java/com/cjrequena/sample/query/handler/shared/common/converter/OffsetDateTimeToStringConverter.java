package com.cjrequena.sample.query.handler.shared.common.converter;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@WritingConverter
public class OffsetDateTimeToStringConverter implements Converter<OffsetDateTime, String> {

    @Override
    public String convert(@NotNull @Nonnull OffsetDateTime source) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        return fmt.format(source);
    }
}
