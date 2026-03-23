package com.cjrequena.sample.command.handler.domain.model.enums;

import com.cjrequena.sample.command.handler.domain.exception.InvalidArgumentException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum ProductType {

  TRANSFER("Transfer"),
  HOTEL("Hotel");

  private final String type;

  @JsonCreator
  public static ProductType from(String type) {
    for (ProductType bookingOrderStatus : ProductType.values()) {
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
