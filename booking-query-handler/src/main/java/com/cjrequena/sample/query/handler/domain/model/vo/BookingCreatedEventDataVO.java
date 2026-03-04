package com.cjrequena.sample.query.handler.domain.model.vo;

import com.cjrequena.sample.query.handler.domain.model.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data payload for BookingCreatedEvent.
 * <p>
 * This class encapsulates the business data of the event, following the
 * CloudEvents specification pattern where event metadata (id, type, source, etc.)
 * is separated from the actual event data payload.
 * </p>
 * <p>
 * This separation provides several benefits:
 * <ul>
 *   <li>Clear distinction between event envelope and event data</li>
 *   <li>Easier serialization and deserialization</li>
 *   <li>Better alignment with CloudEvents specification</li>
 *   <li>Simplified event versioning and evolution</li>
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
public record BookingCreatedEventDataVO(

  @NotNull(message = "Booking ID is required")
  UUID bookingId,

  @NotBlank(message = "Booking reference is required and cannot be blank")
  @Size(min = 1, max = 50, message = "Booking reference must be between 1 and 50 characters")
  String bookingReference,

  @NotNull(message = "Booking status is required")
  BookingStatus status,

  @NotEmpty(message = "At least one passenger is required")
  @Valid
  List<PaxVO> paxes,

  @NotNull(message = "Lead passenger ID is required")
  UUID leadPaxId,

  @NotEmpty(message = "At least one product is required")
  @Valid
  List<ProductVO> products,

  @Valid
  Map<String, Object> metadata

) implements Serializable {
}
