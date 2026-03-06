package com.cjrequena.sample.query.handler.domain.model.vo;


import com.cjrequena.sample.query.handler.domain.model.enums.ProductStatus;
import com.cjrequena.sample.query.handler.domain.model.enums.ProductType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Value object containing common product metadata.
 * <p>
 * This VO encapsulates attributes shared by all product types,
 * following the composition over inheritance principle.
 * </p>
 *
 * @author cjrequena
 */
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({
  "product_id",
  "search_id",
  "search_created_at",
  "product_type",
  "status",
  "paxes_ids",
})
public record ProductMetadataVO(

  @NotNull(message = "Product ID is required")
  UUID productId,

  @NotNull(message = "Search ID is required")
  UUID searchId,

  @NotNull(message = "Search created at is required")
  OffsetDateTime searchCreatedAt,

  @NotNull(message = "Product type is required")
  ProductType productType,

  @Valid
  ProductStatus status,

  @NotNull(message = "Paxes IDs are required")
  List<UUID> paxesIds
) implements Serializable {

  /**
   * Creates a new instance with updated status.
   * Since this is immutable, returns a new instance.
   *
   * @param newStatus the new status
   * @return new ProductMetadataVO with updated status
   */
  public ProductMetadataVO withStatus(ProductStatus newStatus) {
    return new ProductMetadataVO(
      productId,
      searchId,
      searchCreatedAt,
      productType,
      newStatus,
      paxesIds
    );
  }

}
