package com.cjrequena.sample.command.handler.domain.model.vo;

import com.cjrequena.sample.command.handler.domain.model.enums.ProductStatus;
import com.cjrequena.sample.command.handler.domain.model.enums.ProductType;
import com.cjrequena.sample.command.handler.domain.model.vo.transfer.TransferVO;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

import static com.cjrequena.sample.command.handler.shared.common.Constant.TRANSFER;

/**
 * Interface representing a product in a booking.
 * <p>
 * This interface provides type safety for product types while
 * avoiding the complexity of abstract base classes. Each product type
 * is an independent value object that implements this interface.
 * </p>
 * <p>
 * Benefits over abstract base class:
 * <ul>
 *   <li>No inheritance complexity</li>
 *   <li>Each product is independent</li>
 *   <li>Composition over inheritance</li>
 *   <li>Simpler value objects</li>
 *   <li>Better pattern matching support</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "product_type",
  visible = true
)
@JsonSubTypes({
  @JsonSubTypes.Type(value = TransferVO.class, name = TRANSFER)
  // Add other product types here as you create them:
  // @JsonSubTypes.Type(value = ActivityVO.class, name = "Activity"),
  // @JsonSubTypes.Type(value = HotelVO.class, name = "Hotel")
})
public interface ProductVO {

  /**
   * Gets the product metadata containing common attributes.
   *
   * @return the product metadata
   */
  ProductMetadataVO metadata();

  /**
   * Convenience method to get the product ID.
   *
   * @return the product ID
   */
  default UUID productId() {
    return metadata().productId();
  }

  /**
   * Convenience method to get the product type.
   *
   * @return the product type
   */
  default ProductType productType() {
    return metadata().productType();
  }

  /**
   * Convenience method to get the product status.
   *
   * @return the product status
   */
  default ProductStatus status() {
    return metadata().status();
  }

  /**
   * Creates a new product instance with updated status.
   * <p>
   * Since products are immutable value objects, this returns a new
   * instance with the updated status.
   * </p>
   *
   * @param newStatus the new status
   * @return a new product instance with updated status
   */
  ProductVO withStatus(ProductStatus newStatus);

}
