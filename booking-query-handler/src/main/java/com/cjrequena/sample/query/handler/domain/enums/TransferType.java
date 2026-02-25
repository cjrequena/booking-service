package com.cjrequena.sample.query.handler.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of transfer types for trip classification.
 * <p>
 * Defines the direction and nature of a transfer trip:
 * <ul>
 *   <li>ONE_WAY: Single direction transfer with no return</li>
 *   <li>OUTBOUND: First leg of a round-trip transfer (going to destination)</li>
 *   <li>INBOUND: Return leg of a round-trip transfer (coming back from destination)</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum TransferType {

  /**
   * One-way transfer - single direction with no return trip.
   * <p>
   * Used for transfers that only go from origin to destination
   * without a return journey.
   * </p>
   */
  ONE_WAY("ONE_WAY"),

  /**
   * Outbound trip - first leg of a round-trip transfer.
   * <p>
   * Represents the journey from the origin to the destination
   * in a round-trip transfer.
   * </p>
   */
  OUTBOUND("OUTBOUND"),

  /**
   * Inbound trip - return leg of a round-trip transfer.
   * <p>
   * Represents the return journey from the destination back
   * to the origin in a round-trip transfer.
   * </p>
   */
  INBOUND("INBOUND");

  private final String type;

  /**
   * Creates a TransferType from its string representation.
   *
   * @param type the string representation of the transfer type
   * @return the corresponding TransferType enum value
   * @throws IllegalArgumentException if the type is not recognized
   */
  @JsonCreator
  public static TransferType from(String type) {
    for (TransferType transferType : TransferType.values()) {
      if (transferType.type.equals(type)) {
        return transferType;
      }
    }
    throw new IllegalArgumentException("Unexpected transfer type '" + type + "'");
  }

  @JsonValue
  @Override
  public String toString() {
    return String.valueOf(type);
  }

}
