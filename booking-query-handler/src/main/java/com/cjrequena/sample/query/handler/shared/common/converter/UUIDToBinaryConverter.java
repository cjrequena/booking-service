package com.cjrequena.sample.query.handler.shared.common.converter;

import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;

import java.util.UUID;

public class UUIDToBinaryConverter implements Converter<UUID, Binary> {
  @Override
  public Binary convert(UUID source) {
    return new Binary(source.toString().getBytes());
  }
}
