package com.cjrequena.sample.query.handler.persistence.mongodb.entity.hotel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.UUID;

/**
 * MongoDB entity representing a hotel room.
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
public class RoomEntity implements Serializable {

  @NotNull(message = "Room ID is required")
  @Field(name = "room_id")
  private UUID roomId;

  @NotBlank(message = "Room type is required")
  @Field(name = "room_type")
  private String roomType;

  @Field(name = "room_description")
  private String roomDescription;

  @NotNull(message = "Quantity is required")
  @Positive(message = "Quantity must be positive")
  @Field(name = "quantity")
  private Integer quantity;

  @NotNull(message = "Max occupancy is required")
  @Positive(message = "Max occupancy must be positive")
  @Field(name = "max_occupancy")
  private Integer maxOccupancy;

}
