package com.cjrequena.sample.command.handler.controller.dto;

import com.cjrequena.sample.command.handler.domain.model.vo.PaxVO;
import com.cjrequena.sample.command.handler.domain.model.vo.ProductVO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for placing a new booking.
 * <p>
 * This DTO is used for REST API communication and will be mapped
 * to a CreateBookingCommand for domain processing.
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateBookingCommandDTO(

  @NotEmpty(message = "At least one passenger is required")
  @Valid
  List<PaxVO> paxes,

  @NotNull(message = "Lead passenger ID is required")
  UUID leadPaxId,

  @NotEmpty(message = "At least one product is required")
  @Valid
  List<ProductVO> products,

  @Schema(description = "Custom metadata as key-value pairs")
  Map<String, Object> metadata

) implements

  Serializable {
}
