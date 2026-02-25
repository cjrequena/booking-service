package com.cjrequena.sample.query.handler.persistence.mongodb.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.UUID;

/**
 * MongoDB entity representing the price for a specific passenger.
 * <p>
 * Associates a price amount with a passenger in the booking.
 * Amount can be zero for free passengers (e.g., infants, promotional fares).
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
public class PaxPriceEntity implements Serializable {

  @NotNull(message = "Pax ID is required")
  @Field(name = "pax_id")
  private UUID paxId;

  @NotBlank(message = "Currency is required")
  @Field(name = "currency")
  private String currency;

  @NotNull(message = "Amount is required")
  @PositiveOrZero(message = "Amount must be zero or positive")
  @Field(name = "amount")
  private Double amount;

}
