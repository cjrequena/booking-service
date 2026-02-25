package com.cjrequena.sample.command.handler.persistence.mongodb.entity.transfer;

import com.cjrequena.sample.command.handler.domain.model.enums.TransferServiceType;
import com.cjrequena.sample.command.handler.persistence.mongodb.entity.PaxPriceEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.List;

/**
 * MongoDB entity representing transfer pricing information.
 * <p>
 * This entity mirrors the domain's TransferPriceVO structure, using
 * TransferServiceType (PRIVATE/SHARED) to represent the pricing model.
 * </p>
 * <p>
 * Pricing Rules:
 * <ul>
 *   <li>PRIVATE: Single price for entire booking, paxPrices optional</li>
 *   <li>SHARED: Individual prices per passenger, paxPrices required</li>
 * </ul>
 * </p>
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
public class TransferPriceEntity implements Serializable {

  @NotNull(message = "Service type is required")
  @Field(name = "service_type")
  private TransferServiceType serviceType;

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

  /**
   * Per-passenger pricing breakdown.
   * Required for SHARED transfers, optional for PRIVATE transfers.
   */
  @Valid
  @Field(name = "pax_prices")
  private List<PaxPriceEntity> paxPrices;

}
