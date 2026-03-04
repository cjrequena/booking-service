package com.cjrequena.sample.query.handler.persistence.mongodb.entity;

import com.cjrequena.sample.query.handler.domain.model.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB entity representing a booking.
 * <p>
 * This is the root aggregate for the booking domain, containing
 * passengers and products associated with the booking.
 * </p>
 * <p>
 * Indexes:
 * <ul>
 *   <li>booking_reference: Unique index for fast lookup by reference</li>
 *   <li>status: Index for filtering bookings by status</li>
 *   <li>lead_pax_id: Index for finding bookings by lead passenger</li>
 *   <li>paxes.email: Index for searching bookings by passenger email</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({
  "booking_id",
  "booking_reference",
  "status",
  "paxes",
  "lead_pax_id",
  "products"
})
@Document(collection = "booking")
public class BookingEntity implements Serializable {

  @NotNull(message = "Booking ID is required")
  @Id
  @Field(name = "booking_id")
  private UUID bookingId;

  @NotBlank(message = "Booking reference is required and cannot be blank")
  @Size(min = 1, max = 50, message = "Booking reference must be between 1 and 50 characters")
  @Field(name = "booking_reference")
  private String bookingReference;

  @NotNull(message = "Booking status is required")
  @Field(name = "status")
  private BookingStatus status;

  @NotEmpty(message = "At least one passenger is required")
  @Valid
  @Field(name = "paxes")
  private List<PaxEntity> paxes;

  @NotNull(message = "Lead passenger ID is required")
  @Field(name = "lead_pax_id")
  private UUID leadPaxId;

  @NotEmpty(message = "At least one product is required")
  @Valid
  @Field(name = "products")
  private List<ProductEntity> products;

  @Field(name = "metadata")
  private transient java.util.Map<String, Object> metadata;
}
