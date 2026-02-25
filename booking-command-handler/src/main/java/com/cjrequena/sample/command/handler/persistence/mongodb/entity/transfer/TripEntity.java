package com.cjrequena.sample.command.handler.persistence.mongodb.entity.transfer;

import com.cjrequena.sample.command.handler.domain.model.enums.TransferType;
import com.cjrequena.sample.command.handler.shared.common.serializer.OffsetDateTimeDeserializer;
import com.cjrequena.sample.command.handler.shared.common.serializer.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * MongoDB entity representing a single trip within a transfer.
 * <p>
 * A trip represents one leg of a transfer (e.g., outbound or inbound)
 * with specific pickup time, vehicle, and transfer type.
 * </p>
 *
 * @author cjrequena
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TripEntity implements Serializable {

  @NotNull(message = "Trip ID is required")
  @Field(name = "trip_id")
  private UUID tripId;

  @NotNull(message = "Pickup datetime is required")
  @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
  @JsonSerialize(using = OffsetDateTimeSerializer.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
  @Field(name = "pickup_datetime")
  private OffsetDateTime pickupDatetime;

  @NotNull(message = "Transfer type is required")
  @Field(name = "transfer_type")
  private TransferType transferType;

  @NotNull(message = "Vehicle is required")
  @Valid
  @Field(name = "vehicle")
  private VehicleEntity vehicle;

}
