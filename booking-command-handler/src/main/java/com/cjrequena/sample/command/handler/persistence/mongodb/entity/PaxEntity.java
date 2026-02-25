package com.cjrequena.sample.command.handler.persistence.mongodb.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.UUID;

/**
 * MongoDB entity representing a passenger (pax) in a booking.
 * <p>
 * Contains all passenger information including personal details,
 * contact information, and documentation.
 * </p>
 *
 * @author cjrequena
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({
  "pax_id",
  "first_name",
  "last_name",
  "email",
  "phone",
  "age",
  "document_type",
  "document_number",
  "pax_type"
})
@ToString
public class PaxEntity implements Serializable {

  @NotNull(message = "Pax ID is required")
  @Field(name = "pax_id")
  private UUID paxId;

  @NotBlank(message = "First name is required")
  @Field(name = "first_name")
  private String firstName;

  @NotBlank(message = "Last name is required")
  @Field(name = "last_name")
  private String lastName;

  @NotBlank(message = "Email is required")
  @Field(name = "email")
  private String email;

  @NotBlank(message = "Phone is required")
  @Field(name = "phone")
  private String phone;

  @NotNull(message = "Age is required")
  @Positive(message = "Age must be positive")
  @Field(name = "age")
  private Integer age;

  @NotBlank(message = "Document type is required")
  @Field(name = "document_type")
  private String documentType;

  @NotBlank(message = "Document number is required")
  @Field(name = "document_number")
  private String documentNumber;

  @NotBlank(message = "Pax type is required")
  @Field(name = "pax_type")
  private String paxType;

}

