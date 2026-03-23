package com.cjrequena.sample.command.handler.domain.model.vo.hotel;

import com.cjrequena.sample.command.handler.domain.exception.InvalidArgumentException;
import com.cjrequena.sample.command.handler.domain.model.enums.ProductStatus;
import com.cjrequena.sample.command.handler.domain.model.vo.LocationVO;
import com.cjrequena.sample.command.handler.domain.model.vo.ProductMetadataVO;
import com.cjrequena.sample.command.handler.domain.model.vo.ProductVO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Value object representing a hotel product.
 * <p>
 * Business Rules:
 * <ul>
 *   <li>Check-out must be after check-in</li>
 *   <li>At least one room is required</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record HotelVO(

  @NotNull(message = "Product metadata is required")
  @Valid
  @JsonUnwrapped
  ProductMetadataVO metadata,

  @NotBlank(message = "Hotel name is required")
  String hotelName,

  @NotBlank(message = "Hotel code is required")
  String hotelCode,

  @NotNull(message = "Location is required")
  @Valid
  LocationVO location,

  @NotNull(message = "Check-in date is required")
  OffsetDateTime checkIn,

  @NotNull(message = "Check-out date is required")
  OffsetDateTime checkOut,

  @NotEmpty(message = "At least one room is required")
  @Valid
  List<RoomVO> rooms,

  @NotNull(message = "Price is required")
  @Valid
  HotelPriceVO price

) implements ProductVO, Serializable {

  public HotelVO {
    if (checkIn != null && checkOut != null && !checkOut.isAfter(checkIn)) {
      throw new InvalidArgumentException("Check-out must be after check-in");
    }
  }

  @Override
  public HotelVO withStatus(ProductStatus newStatus) {
    return new HotelVO(
      metadata.withStatus(newStatus),
      hotelName,
      hotelCode,
      location,
      checkIn,
      checkOut,
      rooms,
      price
    );
  }

}
