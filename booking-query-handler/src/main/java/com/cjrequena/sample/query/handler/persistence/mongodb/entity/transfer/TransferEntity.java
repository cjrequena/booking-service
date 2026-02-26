package com.cjrequena.sample.query.handler.persistence.mongodb.entity.transfer;

import com.cjrequena.sample.query.handler.domain.enums.ProductStatus;
import com.cjrequena.sample.query.handler.domain.enums.ProductType;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.ProductEntity;
import com.cjrequena.sample.query.handler.shared.common.serializer.OffsetDateTimeDeserializer;
import com.cjrequena.sample.query.handler.shared.common.serializer.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB entity representing a transfer product.
 * <p>
 * This entity represents a transportation service between two locations.
 * </p>
 * <p>
 * Transfer Types:
 * <ul>
 *   <li>One-way: Single trip from origin to destination (returnTrip is null)</li>
 *   <li>Round-trip: Includes both outbound and return trips</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({
  "product_id",
  "search_id",
  "search_created_at",
  "product_type",
  "status",
  "paxes_ids",
  "origin",
  "destination",
  "departure_trip",
  "return_trip",
  "price"
})
@Schema(description = "Represents a transfer product")
@TypeAlias("transfer")
@Document(collection = "products")
public class TransferEntity extends ProductEntity {

  @NotNull(message = "Origin location is required")
  @Valid
  @Field(name = "origin")
  private LocationEntity origin;

  @NotNull(message = "Destination location is required")
  @Valid
  @Field(name = "destination")
  private LocationEntity destination;

  @NotNull(message = "Departure trip is required")
  @Valid
  @Field(name = "departure_trip")
  private TripEntity departureTrip;

  /**
   * Return trip - only present for round-trip transfers.
   * Null for one-way transfers.
   */
  @Valid
  @Field(name = "return_trip")
  private TripEntity returnTrip;

  @NotNull(message = "Price is required")
  @Valid
  @Field(name = "price")
  private TransferPriceEntity price;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public TransferEntity(
    @JsonProperty("product_id") UUID productId,
    @JsonProperty("search_id") UUID searchId,
    @JsonProperty("search_created_at")
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXXX") OffsetDateTime searchCreatedAt,
    @JsonProperty("product_type") ProductType productType,
    @JsonProperty("status") ProductStatus status,
    @JsonProperty("paxes_ids") List<UUID> paxesIds,
    @JsonProperty("origin") LocationEntity origin,
    @JsonProperty("destination") LocationEntity destination,
    @JsonProperty("departure_trip") TripEntity departureTrip,
    @JsonProperty("return_trip") TripEntity returnTrip,
    @JsonProperty("price") TransferPriceEntity price
  ) {
    super(productId, searchId, searchCreatedAt, productType, status, paxesIds);
    this.origin = origin;
    this.destination = destination;
    this.departureTrip = departureTrip;
    this.returnTrip = returnTrip;
    this.price = price;
  }

}
