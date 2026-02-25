package com.cjrequena.sample.command.handler.persistence.mongodb.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;

/**
 * MongoDB entity representing a geographical location.
 * <p>
 * Encapsulates location information including coordinates,
 * airport codes, and address details.
 * </p>
 *
 * @author cjrequena
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LocationEntity implements Serializable {

  @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
  @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
  @Field(name = "latitude")
  private double latitude;

  @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
  @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
  @Field(name = "longitude")
  private double longitude;

  @Field(name = "iata_code")
  private String iataCode;

  @Field(name = "icao_code")
  private String icaoCode;

  @Field(name = "area_code")
  private String areaCode;

  @Field(name = "full_address")
  private String fullAddress;

  @Field(name = "formatted_address")
  private String formattedAddress;

}

