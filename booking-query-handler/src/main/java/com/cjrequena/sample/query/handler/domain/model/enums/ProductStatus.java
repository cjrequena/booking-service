package com.cjrequena.sample.query.handler.domain.model.enums;

import com.cjrequena.sample.query.handler.domain.exception.InvalidArgumentException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum ProductStatus {

  PENDING("PENDING"),
  ACCEPTED("ACCEPTED"),
  CONFIRMED("CONFIRMED"),
  CANCELLED("CANCELLED"),
  REJECTED("REJECTED");

  private final String status;

  @JsonCreator
  public static ProductStatus from(String status) {
    for (ProductStatus bookingOrderStatus : ProductStatus.values()) {
      if (bookingOrderStatus.status.equals(status)) {
        return bookingOrderStatus;
      }
    }
    throw new InvalidArgumentException("Unexpected status '" + status + "'");
  }

  @JsonValue
  @Override
  public String toString() {
    return String.valueOf(status);
  }

}
