package com.cjrequena.sample.query.handler.domain.enums;

import com.cjrequena.sample.query.handler.domain.exception.InvalidArgumentException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum PriceType {

  PER_VEHICLE("PER_VEHICLE"),
  PER_PAX("PER_PAX");

  private final String type;

  @JsonCreator
  public static PriceType from(String type) {
    for (PriceType bookingOrderStatus : PriceType.values()) {
      if (bookingOrderStatus.type.equals(type)) {
        return bookingOrderStatus;
      }
    }
    throw new InvalidArgumentException("Unexpected type '" + type + "'");
  }

  @JsonValue
  @Override
  public String toString() {
    return String.valueOf(type);
  }

}
