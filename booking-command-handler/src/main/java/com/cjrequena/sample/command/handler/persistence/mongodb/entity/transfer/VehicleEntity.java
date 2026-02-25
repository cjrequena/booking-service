package com.cjrequena.sample.command.handler.persistence.mongodb.entity.transfer;

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
 * MongoDB entity representing a vehicle used for transfers.
 * <p>
 * Encapsulates all vehicle-related information including capacity,
 * specifications, and identification details.
 * </p>
 *
 * @author cjrequena
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VehicleEntity implements Serializable {

  @NotNull(message = "Vehicle ID is required")
  @Field(name = "vehicle_id")
  private UUID vehicleId;

  @NotBlank(message = "Vehicle type is required")
  @Field(name = "type")
  private String type;

  @NotBlank(message = "Vehicle description is required")
  @Field(name = "description")
  private String description;

  @NotBlank(message = "Vehicle model is required")
  @Field(name = "model")
  private String model;

  @Field(name = "image")
  private String image;

  @NotNull(message = "Vehicle capacity is required")
  @Positive(message = "Vehicle capacity must be positive")
  @Field(name = "capacity")
  private Integer capacity;

  @NotNull(message = "Maximum bags is required")
  @Positive(message = "Maximum bags must be positive")
  @Field(name = "max_bags")
  private Integer maxBags;

  @NotNull(message = "Maximum passengers is required")
  @Positive(message = "Maximum passengers must be positive")
  @Field(name = "max_paxes")
  private Integer maxPaxes;

}
