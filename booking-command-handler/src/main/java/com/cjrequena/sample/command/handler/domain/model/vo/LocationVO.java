package com.cjrequena.sample.command.handler.domain.model.vo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

/**
 * Value object representing a geographical location.
 * <p>
 * Encapsulates location information including coordinates,
 * airport codes, and address details.
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
public record LocationVO(

  @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
  @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
  double latitude,

  @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
  @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
  double longitude,

  String iataCode,

  String icaoCode,

  String areaCode,

  String fullAddress,

  String formattedAddress

) implements Serializable {

}
