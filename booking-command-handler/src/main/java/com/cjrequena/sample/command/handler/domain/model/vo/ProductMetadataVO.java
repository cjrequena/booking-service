package com.cjrequena.sample.command.handler.domain.model.vo;

import com.cjrequena.sample.command.handler.domain.model.enums.ProductStatus;
import com.cjrequena.sample.command.handler.domain.model.enums.ProductType;
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
public record ProductMetadataVO(

  @NotNull(message = "Product ID is required")
  UUID productId,

  @NotNull(message = "Search ID is required")
  UUID searchId,

  @NotNull(message = "Search created at is required")
  OffsetDateTime searchCreatedAt,

  @NotNull(message = "Product type is required")
  ProductType productType,

  @NotNull(message = "Product status is required")
  ProductStatus status,

  @NotNull(message = "Paxes IDs are required")
  List<UUID> paxesIds,

  @NotNull(message = "Hash is required")
  String hash

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
      paxesIds,
      hash
    );
  }

}
