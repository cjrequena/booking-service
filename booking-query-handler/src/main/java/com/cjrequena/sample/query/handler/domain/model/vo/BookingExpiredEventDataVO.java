package com.cjrequena.sample.query.handler.domain.model.vo;

import com.cjrequena.sample.query.handler.domain.model.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.UUID;

/**
 * Data payload for BookingExpiredEvent.
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BookingExpiredEventDataVO(

  @NotNull(message = "Booking ID is required")
  UUID bookingId,

  @NotBlank(message = "Booking reference is required and cannot be blank")
  @Size(min = 1, max = 50, message = "Booking reference must be between 1 and 50 characters")
  String bookingReference,

  @NotNull(message = "Booking status is required")
  BookingStatus status

) implements Serializable {
}
