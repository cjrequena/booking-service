package com.cjrequena.sample.command.handler.controller;

import com.cjrequena.sample.command.handler.controller.dto.*;
import com.cjrequena.sample.command.handler.controller.exception.BadRequestException;
import com.cjrequena.sample.command.handler.controller.exception.ConflictException;
import com.cjrequena.sample.command.handler.controller.exception.NotFoundException;
import com.cjrequena.sample.command.handler.controller.exception.NotImplementedException;
import com.cjrequena.sample.command.handler.domain.exception.CommandHandlerNotFoundException;
import com.cjrequena.sample.command.handler.domain.exception.PaxPriceException;
import com.cjrequena.sample.command.handler.domain.mapper.CommandMapper;
import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.service.command.CommandBusService;
import com.cjrequena.sample.command.handler.shared.common.Constant;
import com.cjrequena.sample.es.core.domain.exception.AggregateNotFoundException;
import com.cjrequena.sample.es.core.domain.exception.OptimisticConcurrencyException;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = BookingCommandController.ENDPOINT, headers = {BookingCommandController.ACCEPT_VERSION})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "Booking Command API", description = "Command operations for booking aggregate management")
public class BookingCommandController {

  public static final String ENDPOINT = "/command-handler/api/bookings";
  public static final String ACCEPT_VERSION = "Accept-Version=" + Constant.VND_BOOKING_COMMAND_HANDLER_V1;

  private final CommandBusService commandBusService;
  private final CommandMapper commandMapper;

  @Operation(
    summary = "Place a new booking",
    description = "Initiates a new booking in PLACED status. This is the first step in the booking lifecycle.",
    parameters = {
      @Parameter(
        name = "Accept-Version",
        required = true,
        in = ParameterIn.HEADER,
        schema = @Schema(type = "string", allowableValues = {Constant.VND_BOOKING_COMMAND_HANDLER_V1})
      )
    }
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "201",
        description = "Booking successfully placed",
        content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommandResponseDTO.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request data or business rule violation"),
      @ApiResponse(responseCode = "409", description = "Optimistic concurrency conflict"),
      @ApiResponse(responseCode = "500", description = "Internal server error"),
      @ApiResponse(responseCode = "501", description = "Command handler not implemented")
    }
  )
  @PostMapping(path = "/place", produces = {APPLICATION_JSON_VALUE})
  public Mono<ResponseEntity<CommandResponseDTO>> place(@Valid @RequestBody PlaceBookingCommandDTO dto) {
    log.info("Placing new booking");
    Command command = commandMapper.toCommand(dto);
    return handleCommand(command, HttpStatus.CREATED);
  }

  @Operation(
    summary = "Create a booking",
    description = "Creates a new booking. Alternative to place operation with different business semantics.",
    parameters = {
      @Parameter(
        name = "Accept-Version",
        required = true,
        in = ParameterIn.HEADER,
        schema = @Schema(type = "string", allowableValues = {Constant.VND_BOOKING_COMMAND_HANDLER_V1})
      )
    }
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "201",
        description = "Booking successfully created",
        content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommandResponseDTO.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request data or business rule violation"),
      @ApiResponse(responseCode = "409", description = "Optimistic concurrency conflict"),
      @ApiResponse(responseCode = "500", description = "Internal server error"),
      @ApiResponse(responseCode = "501", description = "Command handler not implemented")
    }
  )
  @PostMapping(path = "/create", produces = {APPLICATION_JSON_VALUE})
  public Mono<ResponseEntity<CommandResponseDTO>> create(@Valid @RequestBody CreateBookingCommandDTO dto) {
    log.info("Creating new booking");
    Command command = commandMapper.toCommand(dto);
    return handleCommand(command, HttpStatus.CREATED);
  }

  @Operation(
    summary = "Confirm a booking",
    description = "Confirms a booking, transitioning it to CONFIRMED status. Booking must be in a confirmable state.",
    parameters = {
      @Parameter(
        name = "Accept-Version",
        required = true,
        in = ParameterIn.HEADER,
        schema = @Schema(type = "string", allowableValues = {Constant.VND_BOOKING_COMMAND_HANDLER_V1})
      ),
      @Parameter(
        name = "bookingId",
        description = "Unique identifier of the booking to confirm",
        required = true,
        example = "123e4567-e89b-12d3-a456-426614174000"
      )
    }
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Booking successfully confirmed",
        content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommandResponseDTO.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid booking state or business rule violation"),
      @ApiResponse(responseCode = "404", description = "Booking not found"),
      @ApiResponse(responseCode = "409", description = "Optimistic concurrency conflict"),
      @ApiResponse(responseCode = "500", description = "Internal server error"),
      @ApiResponse(responseCode = "501", description = "Command handler not implemented")
    }
  )
  @PostMapping(path = "/{bookingId}/confirm", produces = {APPLICATION_JSON_VALUE})
  public Mono<ResponseEntity<CommandResponseDTO>> confirm(
    @PathVariable("bookingId") UUID bookingId
  ) {
    log.info("Confirming booking: {}", bookingId);
    ConfirmBookingCommandDTO dto = ConfirmBookingCommandDTO.builder().bookingId(bookingId).build();
    Command command = commandMapper.toCommand(dto);
    return handleCommand(command, HttpStatus.OK);
  }

  @Operation(
    summary = "Cancel a booking",
    description = "Cancels a booking, transitioning it to CANCELLED status. Booking must be in a cancellable state.",
    parameters = {
      @Parameter(
        name = "Accept-Version",
        required = true,
        in = ParameterIn.HEADER,
        schema = @Schema(type = "string", allowableValues = {Constant.VND_BOOKING_COMMAND_HANDLER_V1})
      ),
      @Parameter(
        name = "bookingId",
        description = "Unique identifier of the booking to cancel",
        required = true,
        example = "123e4567-e89b-12d3-a456-426614174000"
      )
    }
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Booking successfully cancelled",
        content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommandResponseDTO.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid booking state or business rule violation"),
      @ApiResponse(responseCode = "404", description = "Booking not found"),
      @ApiResponse(responseCode = "409", description = "Optimistic concurrency conflict"),
      @ApiResponse(responseCode = "500", description = "Internal server error"),
      @ApiResponse(responseCode = "501", description = "Command handler not implemented")
    }
  )
  @PostMapping(path = "/{bookingId}/cancel", produces = {APPLICATION_JSON_VALUE})
  public Mono<ResponseEntity<CommandResponseDTO>> cancel(
    @PathVariable("bookingId") UUID bookingId
  ) {
    log.info("Cancelling booking: {}", bookingId);
    CancelBookingCommandDTO dto = CancelBookingCommandDTO.builder().bookingId(bookingId).build();
    Command command = commandMapper.toCommand(dto);
    return handleCommand(command, HttpStatus.OK);
  }

  @Operation(
    summary = "Complete a booking",
    description = "Completes a booking, transitioning it to COMPLETED status. Booking must be in a completable state.",
    parameters = {
      @Parameter(
        name = "Accept-Version",
        required = true,
        in = ParameterIn.HEADER,
        schema = @Schema(type = "string", allowableValues = {Constant.VND_BOOKING_COMMAND_HANDLER_V1})
      ),
      @Parameter(
        name = "bookingId",
        description = "Unique identifier of the booking to complete",
        required = true,
        example = "123e4567-e89b-12d3-a456-426614174000"
      )
    }
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Booking successfully completed",
        content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommandResponseDTO.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid booking state or business rule violation"),
      @ApiResponse(responseCode = "404", description = "Booking not found"),
      @ApiResponse(responseCode = "409", description = "Optimistic concurrency conflict"),
      @ApiResponse(responseCode = "500", description = "Internal server error"),
      @ApiResponse(responseCode = "501", description = "Command handler not implemented")
    }
  )
  @PostMapping(path = "/{bookingId}/complete", produces = {APPLICATION_JSON_VALUE})
  public Mono<ResponseEntity<CommandResponseDTO>> complete(
    @PathVariable("bookingId") UUID bookingId
  ) {
    log.info("Completing booking: {}", bookingId);
    CompleteBookingCommandDTO dto = CompleteBookingCommandDTO.builder().bookingId(bookingId).build();
    Command command = commandMapper.toCommand(dto);
    return handleCommand(command, HttpStatus.OK);
  }

  @Operation(
    summary = "Expire a booking",
    description = "Expires a booking, transitioning it to EXPIRED status. Typically used for automated expiration processes.",
    parameters = {
      @Parameter(
        name = "Accept-Version",
        required = true,
        in = ParameterIn.HEADER,
        schema = @Schema(type = "string", allowableValues = {Constant.VND_BOOKING_COMMAND_HANDLER_V1})
      ),
      @Parameter(
        name = "bookingId",
        description = "Unique identifier of the booking to expire",
        required = true,
        example = "123e4567-e89b-12d3-a456-426614174000"
      )
    }
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Booking successfully expired",
        content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = CommandResponseDTO.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid booking state or business rule violation"),
      @ApiResponse(responseCode = "404", description = "Booking not found"),
      @ApiResponse(responseCode = "409", description = "Optimistic concurrency conflict"),
      @ApiResponse(responseCode = "500", description = "Internal server error"),
      @ApiResponse(responseCode = "501", description = "Command handler not implemented")
    }
  )
  @PostMapping(path = "/{bookingId}/expire", produces = {APPLICATION_JSON_VALUE})
  public Mono<ResponseEntity<CommandResponseDTO>> expire(
    @PathVariable("bookingId") UUID bookingId
  ) {
    log.info("Expiring booking: {}", bookingId);
    ExpireBookingCommandDTO dto = ExpireBookingCommandDTO.builder().bookingId(bookingId).build();
    Command command = commandMapper.toCommand(dto);
    return handleCommand(command, HttpStatus.OK);
  }

  /**
   * Centralized command handling logic to eliminate code duplication.
   * Handles command execution, response building, and error translation.
   *
   * @param command       the domain command
   * @param successStatus the HTTP status to return on success
   * @return Mono containing the response entity
   */
  private Mono<ResponseEntity<CommandResponseDTO>> handleCommand(Command command, HttpStatus successStatus) {
    try {
      Booking booking = (Booking) commandBusService.handle(command);

      CommandResponseDTO responseDTO = CommandResponseDTO
        .builder()
        .bookingId(booking.getBookingId())
        .status(booking.getStatus())
        .build();

      log.info("Command executed successfully - Booking ID: {}, Status: {}", booking.getBookingId(), booking.getStatus());

      return Mono.just(
        ResponseEntity
          .status(successStatus)
          .cacheControl(CacheControl.noStore().mustRevalidate())
          .header("Booking-Id", command.getAggregateId().toString())
          .body(responseDTO)
      );

    } catch (OptimisticConcurrencyException ex) {
      log.warn("Optimistic concurrency conflict: {}", ex.getMessage());
      throw new ConflictException(ex.getMessage());
    } catch (CommandHandlerNotFoundException ex) {
      log.error("Command handler not found: {}", ex.getMessage());
      throw new NotImplementedException(ex.getMessage());
    } catch (PaxPriceException ex) {
      log.warn("Business rule violation: {}", ex.getMessage());
      throw new BadRequestException(ex.getMessage());
    } catch (AggregateNotFoundException ex) {
      throw new NotFoundException(ex.getMessage());
    } catch (Exception ex) {
      log.error("Unexpected error handling command", ex);
      throw ex;
    }
  }
}
