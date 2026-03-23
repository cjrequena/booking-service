package com.cjrequena.sample.command.handler.domain.model.vo.hotel;

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
 * Value object representing a hotel room.
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RoomVO(

  @NotNull(message = "Room ID is required")
  UUID roomId,

  @NotBlank(message = "Room type is required")
  String roomType,

  String roomDescription,

  @NotNull(message = "Quantity is required")
  @Positive(message = "Quantity must be positive")
  Integer quantity,

  @NotNull(message = "Max occupancy is required")
  @Positive(message = "Max occupancy must be positive")
  Integer maxOccupancy

) implements Serializable {

}
