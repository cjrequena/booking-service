package com.cjrequena.sample.command.handler.domain.model.vo.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.UUID;

/**
 * Value object representing a vehicle used for transfers.
 * <p>
 * Encapsulates all vehicle-related information including capacity,
 * specifications, and identification details. This is an immutable
 * value object that describes vehicle characteristics.
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VehicleVO(
  
  @NotNull(message = "Vehicle ID is required")
  UUID vehicleId,
  
  @NotBlank(message = "Vehicle type is required")
  String type,
  
  @NotBlank(message = "Vehicle description is required")
  String description,
  
  @NotBlank(message = "Vehicle model is required")
  String model,
  
  String image,
  
  @NotNull(message = "Vehicle capacity is required")
  @Positive(message = "Vehicle capacity must be positive")
  Integer capacity,
  
  @NotNull(message = "Maximum bags is required")
  @Positive(message = "Maximum bags must be positive")
  Integer maxBags,
  
  @NotNull(message = "Maximum passengers is required")
  @Positive(message = "Maximum passengers must be positive")
  Integer maxPaxes
  
) implements Serializable {

  /**
   * Validates that the vehicle can accommodate the requested number of passengers and bags.
   *
   * @param requestedPaxes number of passengers
   * @param requestedBags number of bags
   * @return true if vehicle can accommodate the request
   */
  public boolean canAccommodate(int requestedPaxes, int requestedBags) {
    return requestedPaxes <= maxPaxes && requestedBags <= maxBags;
  }

}
