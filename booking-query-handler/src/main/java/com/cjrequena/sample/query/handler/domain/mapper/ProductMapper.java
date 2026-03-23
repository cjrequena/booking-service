package com.cjrequena.sample.query.handler.domain.mapper;

import com.cjrequena.sample.query.handler.domain.exception.MapperException;
import com.cjrequena.sample.query.handler.domain.model.vo.ProductVO;
import com.cjrequena.sample.query.handler.domain.model.vo.hotel.HotelVO;
import com.cjrequena.sample.query.handler.domain.model.vo.transfer.TransferVO;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.ProductEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.hotel.HotelEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.transfer.TransferEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static com.cjrequena.sample.query.handler.shared.common.Constant.HOTEL;
import static com.cjrequena.sample.query.handler.shared.common.Constant.TRANSFER;

/**
 * MapStruct mapper for converting between Product domain VOs and persistence entities.
 * <p>
 * This mapper handles the structural differences between the domain layer
 * (which uses composition with ProductMetadataVO) and the persistence layer
 * (which uses inheritance with ProductEntity base class).
 * </p>
 * <p>
 * Key Mapping Considerations:
 * <ul>
 *   <li>Domain uses @JsonUnwrapped ProductMetadataVO for product metadata</li>
 *   <li>Persistence uses inheritance with ProductEntity superclass</li>
 *   <li>Explicit mappings are required for metadata fields</li>
 *   <li>Nested objects (Location, Trip, Vehicle, etc.) are mapped automatically</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Mapper(
  componentModel = "spring",
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface ProductMapper {

  Logger log = LoggerFactory.getLogger(ProductMapper.class);

  // ================================================================
  // VOs  <-->  Mongo Entities
  // ================================================================

  /**
   * Maps TransferVO to TransferEntity.
   * <p>
   * Explicit mappings are required for metadata fields because the domain
   * uses composition (@JsonUnwrapped ProductMetadataVO) while persistence
   * uses inheritance (extends ProductEntity).
   * </p>
   *
   * @param transferVO the domain transfer value object
   * @return the persistence transfer entity
   */
  @Mapping(source = "metadata.productId", target = "productId")
  @Mapping(source = "metadata.searchId", target = "searchId")
  @Mapping(source = "metadata.searchCreatedAt", target = "searchCreatedAt")
  @Mapping(source = "metadata.productType", target = "productType")
  @Mapping(source = "metadata.status", target = "status")
  @Mapping(source = "metadata.paxesIds", target = "paxesIds")
  TransferEntity toTransfer(TransferVO transferVO);

  @Mapping(source = "metadata.productId", target = "productId")
  @Mapping(source = "metadata.searchId", target = "searchId")
  @Mapping(source = "metadata.searchCreatedAt", target = "searchCreatedAt")
  @Mapping(source = "metadata.productType", target = "productType")
  @Mapping(source = "metadata.status", target = "status")
  @Mapping(source = "metadata.paxesIds", target = "paxesIds")
  HotelEntity toHotel(HotelVO hotelVO);

  /**
   * Maps a generic ProductVO to its corresponding ProductEntity.
   * <p>
   * Uses polymorphism to determine the specific product type and
   * delegates to the appropriate mapper method.
   * </p>
   *
   * @param productVO the domain product value object
   * @return the persistence product entity
   * @throws MapperException if the product type is unknown
   */
  default ProductEntity toProduct(ProductVO productVO) {
    final String productType = productVO.productType().toString();

    switch (productType) {
      case TRANSFER:
        return toTransfer((TransferVO) productVO);
      case HOTEL:
        return toHotel((HotelVO) productVO);
      default:
        String errorMessage = String.format("Error mapping to ProductEntity, unknown product type: %s", productVO.productType());
        log.error(errorMessage);
        throw new MapperException(errorMessage);
    }
  }

  /**
   * Maps a list of ProductVOs to a list of ProductEntity.
   *
   * @param productVOList the list of domain product value objects
   * @return the list of persistence product entities
   */
  default List<ProductEntity> toProductList(List<ProductVO> productVOList) {
    return productVOList
      .stream()
      .map(this::toProduct)
      .collect(Collectors.toList());
  }

}
