package com.cjrequena.sample.command.handler.domain.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.util.UUID;

/**
 * Value object representing a passenger (pax) in a booking.
 * <p>
 * Contains all passenger information including personal details,
 * contact information, and documentation.
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
public record PaxVO(

  @NotNull(message = "Pax ID is required")
  UUID paxId,

  @NotBlank(message = "First name is required")
  String firstName,

  @NotBlank(message = "Last name is required")
  String lastName,

  @NotBlank(message = "Email is required")
  String email,

  @NotBlank(message = "Phone is required")
  String phone,

  @NotNull(message = "Age is required")
  @Positive(message = "Age must be positive")
  Integer age,

  @NotBlank(message = "Document type is required")
  String documentType,

  @NotBlank(message = "Document number is required")
  String documentNumber,

  @NotBlank(message = "Pax type is required")
  String paxType

) implements Serializable {

}
