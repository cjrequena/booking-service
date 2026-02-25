package com.cjrequena.sample.command.handler.domain.model.vo.transfer;

import com.cjrequena.sample.command.handler.domain.exception.InvalidArgumentException;
import com.cjrequena.sample.command.handler.domain.model.enums.ProductStatus;
import com.cjrequena.sample.command.handler.domain.model.enums.TransferType;
import com.cjrequena.sample.command.handler.domain.model.vo.LocationVO;
import com.cjrequena.sample.command.handler.domain.model.vo.ProductMetadataVO;
import com.cjrequena.sample.command.handler.domain.model.vo.ProductVO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.Optional;

/**
 * Value object representing a transfer product.
 * <p>
 * This implementation uses composition instead of inheritance:
 * <ul>
 *   <li>Composes ProductMetadataVO for common attributes</li>
 *   <li>Implements ProductVO interface for type safety</li>
 *   <li>Simple, clean value object without abstract class complexity</li>
 * </ul>
 * </p>
 * <p>
 * A transfer is a transportation service between two locations, which can be:
 * <ul>
 *   <li>One-way: Single trip from origin to destination</li>
 *   <li>Round-trip: Includes both outbound and return trips</li>
 * </ul>
 * </p>
 * <p>
 * Business Rules:
 * <ul>
 *   <li>Departure trip is always required</li>
 *   <li>Return trip is optional (null for one-way transfers)</li>
 *   <li>If return trip exists, it must be after departure trip</li>
 *   <li>Departure trip must be ONE_WAY or OUTBOUND type</li>
 *   <li>Return trip must be INBOUND type</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TransferVO(

  /**
   * Product metadata containing common attributes.
   * Uses @JsonUnwrapped to flatten the JSON structure, maintaining
   * backward compatibility with the previous flat structure.
   */
  @NotNull(message = "Product metadata is required")
  @Valid
  @JsonUnwrapped
  ProductMetadataVO metadata,

  @NotNull(message = "Origin location is required")
  @Valid
  LocationVO origin,

  @NotNull(message = "Destination location is required")
  @Valid
  LocationVO destination,

  @NotNull(message = "Departure trip is required")
  @Valid
  TripVO departureTrip,

  /**
   * Return trip - only present for round-trip transfers.
   * Null for one-way transfers.
   */
  @Valid
  TripVO returnTrip,

  @NotNull(message = "Price is required")
  @Valid
  TransferPriceVO price

) implements ProductVO, Serializable {

  /**
   * Canonical constructor with business rule validation.
   */
  public TransferVO {
    // Validate departure trip type
    if (departureTrip != null) {
      TransferType depType = departureTrip.transferType();
      if (depType != TransferType.ONE_WAY && depType != TransferType.OUTBOUND) {
        throw new InvalidArgumentException(
          "Departure trip must be ONE_WAY or OUTBOUND, but was: " + depType
        );
      }
    }

    // Validate return trip consistency
    if (returnTrip != null) {
      TransferType retType = returnTrip.transferType();
      if (retType != TransferType.INBOUND) {
        throw new InvalidArgumentException(
          "Return trip must be INBOUND, but was: " + retType
        );
      }

      // Validate return trip is after departure
      if (departureTrip != null && 
          !returnTrip.pickupDatetime().isAfter(departureTrip.pickupDatetime())) {
        throw new InvalidArgumentException(
          "Return trip pickup must be after departure trip pickup"
        );
      }
    }
  }

  /**
   * Checks if this is a round-trip transfer.
   *
   * @return true if return trip is present
   */
  @com.fasterxml.jackson.annotation.JsonIgnore
  public boolean isRoundTrip() {
    return returnTrip != null;
  }

  /**
   * Checks if this is a one-way transfer.
   *
   * @return true if return trip is not present
   */
  @com.fasterxml.jackson.annotation.JsonIgnore
  public boolean isOneWay() {
    return returnTrip == null;
  }

  /**
   * Gets the return trip if present.
   *
   * @return Optional containing the return trip, or empty if one-way
   */
  @com.fasterxml.jackson.annotation.JsonIgnore
  public Optional<TripVO> getReturnTrip() {
    return Optional.ofNullable(returnTrip);
  }

  /**
   * Creates a new TransferVO with updated status.
   * <p>
   * Since VOs are immutable, this returns a new instance with the updated status.
   * </p>
   *
   * @param newStatus the new status
   * @return a new TransferVO instance with updated status
   */
  @Override
  public TransferVO withStatus(ProductStatus newStatus) {
    return new TransferVO(
      metadata.withStatus(newStatus),
      origin,
      destination,
      departureTrip,
      returnTrip,
      price
    );
  }

}
