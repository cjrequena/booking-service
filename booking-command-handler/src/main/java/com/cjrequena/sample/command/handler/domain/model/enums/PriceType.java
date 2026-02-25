package com.cjrequena.sample.command.handler.domain.model.enums;

import com.cjrequena.sample.command.handler.domain.exception.InvalidArgumentException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @deprecated Use {@link TransferServiceType} instead.
 * This enum is deprecated in favor of TransferServiceType which better
 * represents the business domain (PRIVATE/SHARED) rather than technical
 * pricing implementation (PER_VEHICLE/PER_PAX).
 */
@Deprecated(since = "0.0.1", forRemoval = true)
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
