package com.cjrequena.sample.command.handler.domain.mapper;

import com.cjrequena.sample.command.handler.controller.dto.PlaceBookingCommandDTO;
import com.cjrequena.sample.command.handler.domain.model.command.PlaceBookingCommand;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * MapStruct mapper for converting between PlaceBookingCommandDTO and PlaceBookingCommand.
 * <p>
 * This mapper handles the conversion between the REST API layer (DTO)
 * and the domain layer (Command), including UUID generation if needed.
 * </p>
 * <p>
 * MapStruct will generate the implementation at compile time, providing
 * type-safe and performant mapping without reflection.
 * </p>
 * <p>
 * Usage:
 * <pre>
 * PlaceBookingCommandMapper mapper = PlaceBookingCommandMapper.INSTANCE;
 * PlaceBookingCommand command = mapper.toCommand(dto);
 * PlaceBookingCommandDTO dto = mapper.toDTO(command);
 * </pre>
 * </p>
 *
 * @author cjrequena
 */

@Mapper(
  componentModel = "spring",
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  uses = {
    ProductMapper.class,
    PaxMapper.class
  }
)
public interface CommandMapper {


  // ================================================================
  // Commands  <-->  DTOs
  // ================================================================
  default PlaceBookingCommand toCommand(PlaceBookingCommandDTO dto) {
    return new PlaceBookingCommand(
      dto.paxes(),
      dto.leadPaxId(),
      dto.products()
    );
  }
}
