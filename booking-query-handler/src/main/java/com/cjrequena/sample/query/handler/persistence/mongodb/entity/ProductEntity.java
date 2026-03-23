package com.cjrequena.sample.query.handler.persistence.mongodb.entity;

import com.cjrequena.sample.query.handler.domain.model.enums.ProductStatus;
import com.cjrequena.sample.query.handler.domain.model.enums.ProductType;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.hotel.HotelEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.transfer.TransferEntity;
import com.cjrequena.sample.query.handler.shared.common.serializer.OffsetDateTimeDeserializer;
import com.cjrequena.sample.query.handler.shared.common.serializer.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.cjrequena.sample.query.handler.shared.common.Constant.HOTEL;
import static com.cjrequena.sample.query.handler.shared.common.Constant.TRANSFER;

/**
 * Abstract base class for product entities in MongoDB.
 * <p>
 * This class represents the product metadata that is common to all product types.
 * It uses inheritance for persistence purposes, which is appropriate for MongoDB's
 * document model and polymorphic queries.
 * </p>
 *
 * @author cjrequena
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@ToString
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "product_id",
  "search_id",
  "search_created_at",
  "product_type",
  "status",
  "paxes_ids"
})
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "product_type",
  visible = true
)
@JsonSubTypes({
  @JsonSubTypes.Type(value = TransferEntity.class, name = TRANSFER),
  @JsonSubTypes.Type(value = HotelEntity.class, name = HOTEL)
})
@Schema(description = "Represents a product")
@Document
@TypeAlias("products")
public abstract class ProductEntity implements Serializable {

  @NotNull(message = "Product ID is required")
  @Field(name = "product_id")
  private UUID productId;

  @NotNull(message = "Search ID is required")
  @Field(name = "search_id")
  private UUID searchId;

  @NotNull(message = "Search created at is required")
  @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
  @JsonSerialize(using = OffsetDateTimeSerializer.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXXX")
  @Field(name = "search_created_at")
  private OffsetDateTime searchCreatedAt;

  @NotNull(message = "Product type is required")
  @Field(name = "product_type")
  private ProductType productType;

  @NotNull(message = "Product status is required")
  @Field(name = "status")
  private ProductStatus status;

  @NotNull(message = "Paxes IDs are required")
  @Field(name = "paxes_ids")
  private List<UUID> paxesIds;

}
