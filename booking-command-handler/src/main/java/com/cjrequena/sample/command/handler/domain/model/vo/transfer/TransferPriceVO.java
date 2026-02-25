package com.cjrequena.sample.command.handler.domain.model.vo.transfer;

import com.cjrequena.sample.command.handler.domain.exception.InvalidArgumentException;
import com.cjrequena.sample.command.handler.domain.model.enums.TransferServiceType;
import com.cjrequena.sample.command.handler.domain.model.vo.PaxPriceVO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Value object representing the pricing structure for a transfer.
 * <p>
 * This model uses TransferServiceType (PRIVATE/SHARED) to clearly represent
 * the business domain and pricing structure:
 * <ul>
 *   <li>PRIVATE transfers: Exclusive vehicle, single price for the entire booking</li>
 *   <li>SHARED transfers: Shared vehicle, individual prices per passenger</li>
 * </ul>
 * </p>
 * <p>
 * This is an immutable value object with validation logic to ensure
 * pricing consistency and correctness based on the service type.
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TransferPriceVO(

  @NotNull(message = "Service type is required")
  TransferServiceType serviceType,

  @NotBlank(message = "Currency is required")
  String currency,

  @NotNull(message = "Total amount is required")
  @PositiveOrZero(message = "Total amount must be zero or positive")
  Double totalAmount,

  @NotNull(message = "Subtotal amount is required")
  @PositiveOrZero(message = "Subtotal amount must be zero or positive")
  Double subtotalAmount,

  @NotNull(message = "Fees and taxes is required")
  @PositiveOrZero(message = "Fees and taxes must be zero or positive")
  Double feesAndTaxes,

  /**
   * Per-passenger pricing breakdown.
   * <ul>
   *   <li>PRIVATE: Optional - can be null or used for informational/accounting purposes</li>
   *   <li>SHARED: Required - must contain price for each passenger</li>
   * </ul>
   */
  @Valid
  List<PaxPriceVO> paxPrices

) implements Serializable {

  /**
   * Canonical constructor with validation.
   */
  public TransferPriceVO {
    // Validate price breakdown consistency
    double calculatedTotal = subtotalAmount + feesAndTaxes;
    if (Math.abs(totalAmount - calculatedTotal) > 0.01) {
      throw new InvalidArgumentException(
        String.format("Total amount (%.2f) must equal subtotal (%.2f) + fees and taxes (%.2f)",
          totalAmount, subtotalAmount, feesAndTaxes)
      );
    }

    // Validate passenger pricing for SHARED transfers
    if (serviceType == TransferServiceType.SHARED) {
      if (paxPrices == null || paxPrices.isEmpty()) {
        throw new InvalidArgumentException(
          "Passenger prices are required for SHARED transfer service"
        );
      }

      // Validate that sum of passenger prices matches subtotal
      double passengerTotal = paxPrices.stream()
        .mapToDouble(PaxPriceVO::amount)
        .sum();

      if (Math.abs(passengerTotal - subtotalAmount) > 0.01) {
        throw new InvalidArgumentException(
          String.format("Sum of passenger prices (%.2f) must equal subtotal (%.2f)",
            passengerTotal, subtotalAmount)
        );
      }

      // Validate currency consistency for SHARED
      boolean allSameCurrency = paxPrices.stream()
        .allMatch(p -> p.currency().equals(currency));

      if (!allSameCurrency) {
        throw new InvalidArgumentException(
          "All passenger prices must use the same currency: " + currency
        );
      }
    }

    // For PRIVATE, if paxPrices provided, validate it's consistent (optional validation)
    if (serviceType == TransferServiceType.PRIVATE && paxPrices != null && !paxPrices.isEmpty()) {
      double passengerTotal = paxPrices.stream()
        .mapToDouble(PaxPriceVO::amount)
        .sum();

      if (Math.abs(passengerTotal - subtotalAmount) > 0.01) {
        throw new InvalidArgumentException(
          String.format("Optional passenger price breakdown (%.2f) must equal subtotal (%.2f)",
            passengerTotal, subtotalAmount)
        );
      }

      // Validate currency consistency
      boolean allSameCurrency = paxPrices.stream()
        .allMatch(p -> p.currency().equals(currency));

      if (!allSameCurrency) {
        throw new InvalidArgumentException(
          "All passenger prices must use the same currency: " + currency
        );
      }
    }

    // Make paxPrices immutable
    paxPrices = paxPrices == null ? null : Collections.unmodifiableList(paxPrices);
  }

  /**
   * Checks if this is a private transfer.
   *
   * @return true if service type is PRIVATE
   */
  @com.fasterxml.jackson.annotation.JsonIgnore
  public boolean isPrivate() {
    return serviceType == TransferServiceType.PRIVATE;
  }

  /**
   * Checks if this is a shared transfer.
   *
   * @return true if service type is SHARED
   */
  @com.fasterxml.jackson.annotation.JsonIgnore
  public boolean isShared() {
    return serviceType == TransferServiceType.SHARED;
  }

  /**
   * Calculates the average price per passenger.
   * <p>
   * For PRIVATE transfers, divides total by passenger count.
   * For SHARED transfers, calculates average from individual prices.
   * </p>
   *
   * @param numberOfPassengers the number of passengers (required for PRIVATE)
   * @return the average price per passenger
   * @throws InvalidArgumentException if numberOfPassengers is invalid
   */
  @com.fasterxml.jackson.annotation.JsonIgnore
  public double calculateAveragePricePerPassenger(int numberOfPassengers) {
    if (numberOfPassengers <= 0) {
      throw new InvalidArgumentException("Number of passengers must be positive");
    }

    if (isShared() && paxPrices != null) {
      // For shared, calculate average from actual passenger prices
      return BigDecimal.valueOf(totalAmount)
        .divide(BigDecimal.valueOf(paxPrices.size()), 2, RoundingMode.HALF_UP)
        .doubleValue();
    }

    // For private, divide total by passenger count
    return BigDecimal.valueOf(totalAmount)
      .divide(BigDecimal.valueOf(numberOfPassengers), 2, RoundingMode.HALF_UP)
      .doubleValue();
  }

  /**
   * Gets the price for a specific passenger (SHARED transfers only).
   *
   * @param paxId the passenger ID
   * @return the price for the passenger, or null if not found
   * @throws InvalidArgumentException if service type is not SHARED
   */
  @com.fasterxml.jackson.annotation.JsonIgnore
  public Double getPriceForPassenger(UUID paxId) {
    if (!isShared()) {
      throw new InvalidArgumentException(
        "Per-passenger price lookup is only valid for SHARED transfers"
      );
    }

    if (paxPrices == null) {
      return null;
    }

    return paxPrices.stream()
      .filter(paxPrice -> paxPrice.paxId().equals(paxId))
      .findFirst()
      .map(PaxPriceVO::amount)
      .orElse(null);
  }

}
