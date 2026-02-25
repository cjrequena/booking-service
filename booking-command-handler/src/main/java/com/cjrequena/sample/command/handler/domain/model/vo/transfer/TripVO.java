package com.cjrequena.sample.command.handler.domain.model.vo.transfer;

import com.cjrequena.sample.command.handler.domain.model.enums.TransferType;
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
