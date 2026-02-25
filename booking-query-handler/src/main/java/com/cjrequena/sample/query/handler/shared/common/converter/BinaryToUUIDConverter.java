package com.cjrequena.sample.query.handler.shared.common.converter;

import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;

import java.util.UUID;

public class BinaryToUUIDConverter implements Converter<Binary, UUID> {
  @Override
  public UUID convert(Binary source) {
    return UUID.nameUUIDFromBytes(source.getData());
  }
}
