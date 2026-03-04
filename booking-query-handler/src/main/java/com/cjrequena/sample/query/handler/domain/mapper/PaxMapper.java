package com.cjrequena.sample.query.handler.domain.mapper;

import com.cjrequena.sample.query.handler.domain.model.vo.PaxVO;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.PaxEntity;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between Pax domain VOs and persistence entities.
 * <p>
 * This mapper handles the conversion between passenger value objects (PaxVO)
 * and passenger entities (PaxEntity). Since both have identical field names and types,
 * MapStruct can generate the mapping automatically without explicit configuration.
 * </p>
 *
 * @author cjrequena
 */
@Mapper(
  componentModel = "spring",
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface PaxMapper {
  Logger log = LoggerFactory.getLogger(PaxMapper.class);

  // ================================================================
  // VOs  <-->  Mongo Entities
  // ================================================================

  /**
   * Maps PaxVO to PaxEntity.
   * <p>
   * All fields have matching names and types, so MapStruct generates
   * the implementation automatically.
   * </p>
   *
   * @param paxVO the domain passenger value object
   * @return the persistence passenger entity
   */
  PaxEntity toPax(PaxVO paxVO);

  /**
   * Maps a list of PaxVOs to a list of PaxEntity.
   *
   * @param paxVOList the list of domain passenger value objects
   * @return the list of persistence passenger entities
   */
  default List<PaxEntity> toPaxList(List<PaxVO> paxVOList) {
    return paxVOList
      .stream()
      .map(this::toPax)
      .collect(Collectors.toList());
  }

}
