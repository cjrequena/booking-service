package com.cjrequena.sample.command.handler.domain.model.vo.transfer;

import com.cjrequena.sample.command.handler.domain.model.enums.TransferType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Value object representing a single trip within a transfer.
 * <p>
 * A trip represents one leg of a transfer (e.g., outbound or inbound)
 * with specific pickup time, vehicle, and transfer type.
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TripVO(

  @NotNull(message = "Trip ID is required")
  UUID tripId,

  @NotNull(message = "Pickup datetime is required")
  OffsetDateTime pickupDatetime,

  @NotNull(message = "Transfer type is required")
  TransferType transferType,

  @NotNull(message = "Vehicle is required")
  @Valid
  VehicleVO vehicle

) implements Serializable {

}
