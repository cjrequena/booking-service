package com.cjrequena.sample.query.handler.domain.model.vo;

import com.cjrequena.sample.query.handler.domain.exception.PaxPriceException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.UUID;

/**
 * Value object representing the price for a specific passenger.
 * <p>
 * Associates a price amount with a passenger in the booking.
 * Amount can be zero for free passengers (e.g., infants, promotional fares).
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaxPriceVO(

  @NotNull(message = "Pax ID is required")
  UUID paxId,

  @NotBlank(message = "Currency is required")
  String currency,

  @NotNull(message = "Amount is required")
  @PositiveOrZero(message = "Amount must be zero or positive")
  Double amount

) implements Serializable {

  /**
   * Canonical constructor with validation.
   */
  public PaxPriceVO {
    if (paxId == null) {
      throw new PaxPriceException("The pax_id is required");
    }
    if (currency == null || currency.isBlank()) {
      throw new PaxPriceException("The currency is required");
    }
    if (amount == null || amount < 0) {
      throw new PaxPriceException("The amount must be zero or positive");
    }
  }

}
