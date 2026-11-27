package com.cjrequena.sample.command.handler.controller.dto;

import com.cjrequena.sample.command.handler.domain.model.enums.BookingStatus;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.UUID;

/**
 * Response returned by every booking command endpoint.
 * <p>
 * JSON Structure (snake_case):
 * <pre>
 * {
 *   "booking_id": "uuid",
 *   "status": "CREATED"
 * }
 * </pre>
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
public record CommandResponseDTO(

  UUID bookingId,

  BookingStatus status

) implements Serializable {
}
