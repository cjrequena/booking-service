package com.cjrequena.sample.command.handler.domain.model.enums;

import com.cjrequena.sample.command.handler.domain.exception.InvalidArgumentException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of transfer service types.
 * <p>
 * Defines whether a transfer is private (exclusive vehicle) or
 * shared (vehicle shared with other passengers):
 * <ul>
 *   <li>PRIVATE: Exclusive vehicle for the booking party, single price regardless of passenger count</li>
 *   <li>SHARED: Shared vehicle with other passengers, individual pricing per passenger</li>
 * </ul>
 * </p>
 * <p>
 * This enum represents the business domain more clearly than technical
 * pricing models (PER_VEHICLE/PER_PAX), making the code more aligned
 * with how transfer services actually operate.
 * </p>
 *
 * @author cjrequena
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum TransferServiceType {

  /**
   * Private transfer - exclusive vehicle for the booking party.
   * <p>
   * Characteristics:
   * <ul>
   *   <li>Single price for the entire vehicle</li>
   *   <li>Price doesn't change with passenger count (within vehicle capacity)</li>
   *   <li>Examples: Private taxi, executive car, private shuttle</li>
   * </ul>
   * </p>
   */
  PRIVATE("PRIVATE"),

  /**
   * Shared transfer - vehicle shared with other passengers.
   * <p>
   * Characteristics:
   * <ul>
   *   <li>Individual pricing per passenger</li>
   *   <li>Each passenger pays their own fare</li>
   *   <li>Different passenger types may have different prices (adult, child, infant)</li>
   *   <li>Examples: Shared shuttle, airport bus, shared van</li>
   * </ul>
   * </p>
   */
  SHARED("SHARED");

  private final String type;

  /**
   * Creates a TransferServiceType from its string representation.
   *
   * @param type the string representation of the service type
   * @return the corresponding TransferServiceType enum value
   * @throws InvalidArgumentException if the type is not recognized
   */
  @JsonCreator
  public static TransferServiceType from(String type) {
    for (TransferServiceType serviceType : TransferServiceType.values()) {
      if (serviceType.type.equals(type)) {
        return serviceType;
      }
    }
    throw new InvalidArgumentException("Unexpected transfer service type '" + type + "'");
  }

  @JsonValue
  @Override
  public String toString() {
    return String.valueOf(type);
  }

}
