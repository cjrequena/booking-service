package com.cjrequena.sample.query.handler.domain.model.vo.hotel;

import com.cjrequena.sample.query.handler.domain.exception.InvalidArgumentException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

/**
 * Value object representing the pricing structure for a hotel stay.
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HotelPriceVO(

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

  @NotNull(message = "Nightly rate is required")
  @PositiveOrZero(message = "Nightly rate must be zero or positive")
  Double nightlyRate,

  @NotNull(message = "Number of nights is required")
  @Positive(message = "Number of nights must be positive")
  Integer nights

) implements Serializable {

  public HotelPriceVO {
    double calculatedTotal = subtotalAmount + feesAndTaxes;
    if (Math.abs(totalAmount - calculatedTotal) > 0.01) {
      throw new InvalidArgumentException(
        String.format("Total amount (%.2f) must equal subtotal (%.2f) + fees and taxes (%.2f)",
          totalAmount, subtotalAmount, feesAndTaxes)
      );
    }
  }

}
