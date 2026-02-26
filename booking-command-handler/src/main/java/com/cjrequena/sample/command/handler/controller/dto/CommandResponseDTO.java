package com.cjrequena.sample.command.handler.controller.dto;

import com.cjrequena.sample.command.handler.domain.model.enums.BookingStatus;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.UUID;

/**
 * Data Transfer Object for placing a new booking.
 * <p>
 * This DTO is used for REST API communication and will be mapped
 * to a PlaceBookingCommand for domain processing.
 * </p>
 * <p>
 * JSON Structure (snake_case):
 * <pre>
 * {
 *   "booking_id": "uuid",
 *   "booking_reference": "BK-2026-001",
 *   "paxes": [...],
 *   "lead_pax_id": "uuid",
 *   "products": [...]
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
