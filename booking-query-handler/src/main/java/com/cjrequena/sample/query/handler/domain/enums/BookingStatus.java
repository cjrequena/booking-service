package com.cjrequena.sample.query.handler.domain.enums;

import com.cjrequena.sample.query.handler.domain.exception.InvalidArgumentException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum BookingStatus {

  CREATED("CREATED"),
  ACCEPTED("ACCEPTED"),
  CONFIRMED("CONFIRMED"),
  CANCELLED("CANCELLED"),
  COMPLETED("COMPLETED"),
  EXPIRED("EXPIRED"),
  PLACED("PLACED");

  //  PAYMENT_INITIATED("PAYMENT_INITIATED"),
  //  PAYMENT_CONFIRMED("PAYMENT_CONFIRMED"),
  //  PAYMENT_FAILED("PAYMENT_FAILED"),
  //  PAYMENT_REFUNDED("PAYMENT_REFUNDED"),
  //  PAYMENT_DISPUTED("PAYMENT_DISPUTED"),
  //  PAYMENT_DISPUTE_RESOLVED("PAYMENT_DISPUTE_RESOLVED");

  private final String status;

  @JsonCreator
  public static BookingStatus from(String status) {
    for (BookingStatus bookingStatus : BookingStatus.values()) {
      if (bookingStatus.status.equals(status)) {
        return bookingStatus;
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
