package com.cjrequena.sample.query.handler.domain.model.enums;

import com.cjrequena.sample.query.handler.domain.exception.InvalidArgumentException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of transfer types.
 * <p>
 * Defines the direction and nature of a transfer trip:
 * <ul>
 *   <li>ONE_WAY: Single direction transfer (e.g., airport to hotel)</li>
 *   <li>ROUND_TRIP: Return transfer included (e.g., airport to hotel and back)</li>
 *   <li>OUTBOUND: Outbound leg of a round trip</li>
 *   <li>INBOUND: Return leg of a round trip</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum TransferType {

  ONE_WAY("ONE_WAY"),
  ROUND_TRIP("ROUND_TRIP"),
  OUTBOUND("OUTBOUND"),
  INBOUND("INBOUND");

  private final String type;

  /**
   * Creates a TransferType from its string representation.
   *
   * @param type the string representation of the transfer type
   * @return the corresponding TransferType enum value
   * @throws InvalidArgumentException if the type is not recognized
   */
  @JsonCreator
  public static TransferType from(String type) {
    for (TransferType transferType : TransferType.values()) {
      if (transferType.type.equals(type)) {
        return transferType;
      }
    }
    throw new InvalidArgumentException("Unexpected transfer type '" + type + "'");
  }

  @JsonValue
  @Override
  public String toString() {
    return String.valueOf(type);
  }

}
