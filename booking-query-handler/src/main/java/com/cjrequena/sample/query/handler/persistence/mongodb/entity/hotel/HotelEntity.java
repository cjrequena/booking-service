package com.cjrequena.sample.query.handler.persistence.mongodb.entity.hotel;

import com.cjrequena.sample.query.handler.domain.model.enums.ProductStatus;
import com.cjrequena.sample.query.handler.domain.model.enums.ProductType;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.ProductEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.transfer.LocationEntity;
import com.cjrequena.sample.query.handler.shared.common.serializer.OffsetDateTimeDeserializer;
import com.cjrequena.sample.query.handler.shared.common.serializer.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
 * MongoDB entity representing a hotel product.
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
  "hotel_name",
  "hotel_code",
  "location",
  "check_in",
  "check_out",
  "rooms",
  "price"
})
@Schema(description = "Represents a hotel product")
@TypeAlias("hotel")
@Document(collection = "products")
public class HotelEntity extends ProductEntity {

  @NotBlank(message = "Hotel name is required")
  @Field(name = "hotel_name")
  private String hotelName;

  @NotBlank(message = "Hotel code is required")
  @Field(name = "hotel_code")
  private String hotelCode;

  @NotNull(message = "Location is required")
  @Valid
  @Field(name = "location")
  private LocationEntity location;

  @NotNull(message = "Check-in date is required")
  @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
  @JsonSerialize(using = OffsetDateTimeSerializer.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXXX")
  @Field(name = "check_in")
  private OffsetDateTime checkIn;

  @NotNull(message = "Check-out date is required")
  @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
  @JsonSerialize(using = OffsetDateTimeSerializer.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXXX")
  @Field(name = "check_out")
  private OffsetDateTime checkOut;

  @NotEmpty(message = "At least one room is required")
  @Valid
  @Field(name = "rooms")
  private List<RoomEntity> rooms;

  @NotNull(message = "Price is required")
  @Valid
  @Field(name = "price")
  private HotelPriceEntity price;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public HotelEntity(
    @JsonProperty("product_id") UUID productId,
    @JsonProperty("search_id") UUID searchId,
    @JsonProperty("search_created_at")
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXXX") OffsetDateTime searchCreatedAt,
    @JsonProperty("product_type") ProductType productType,
    @JsonProperty("status") ProductStatus status,
    @JsonProperty("paxes_ids") List<UUID> paxesIds,
    @JsonProperty("hotel_name") String hotelName,
    @JsonProperty("hotel_code") String hotelCode,
    @JsonProperty("location") LocationEntity location,
    @JsonProperty("check_in") OffsetDateTime checkIn,
    @JsonProperty("check_out") OffsetDateTime checkOut,
    @JsonProperty("rooms") List<RoomEntity> rooms,
    @JsonProperty("price") HotelPriceEntity price
  ) {
    super(productId, searchId, searchCreatedAt, productType, status, paxesIds);
    this.hotelName = hotelName;
    this.hotelCode = hotelCode;
    this.location = location;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.rooms = rooms;
    this.price = price;
  }

}
