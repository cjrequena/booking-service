package com.cjrequena.sample.query.handler.persistence.mongodb.entity.hotel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;

/**
 * MongoDB entity representing hotel pricing information.
 *
 * @author cjrequena
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HotelPriceEntity implements Serializable {

  @NotBlank(message = "Currency is required")
  @Field(name = "currency")
  private String currency;

  @NotNull(message = "Total amount is required")
  @PositiveOrZero(message = "Total amount must be zero or positive")
  @Field(name = "total_amount")
  private Double totalAmount;

  @NotNull(message = "Subtotal amount is required")
  @PositiveOrZero(message = "Subtotal amount must be zero or positive")
  @Field(name = "subtotal_amount")
  private Double subtotalAmount;

  @NotNull(message = "Fees and taxes is required")
  @PositiveOrZero(message = "Fees and taxes must be zero or positive")
  @Field(name = "fees_and_taxes")
  private Double feesAndTaxes;

  @NotNull(message = "Nightly rate is required")
  @PositiveOrZero(message = "Nightly rate must be zero or positive")
  @Field(name = "nightly_rate")
  private Double nightlyRate;

  @NotNull(message = "Number of nights is required")
  @Positive(message = "Number of nights must be positive")
  @Field(name = "nights")
  private Integer nights;

}
