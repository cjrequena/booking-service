package com.cjrequena.sample.command.handler.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.UUID;

/**
 * Data Transfer Object for confirming a booking.
 * <p>
 * This DTO is used for REST API communication and will be mapped
 * to a ConfirmBookingCommand for domain processing.
 * </p>
 * <p>
 * JSON Structure (snake_case):
 * <pre>
 * {
 *   "booking_id": "uuid"
 * }
 * </pre>
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ConfirmBookingCommandDTO(

  @NotNull(message = "Booking ID is required")
  UUID bookingId

) implements Serializable {
}
