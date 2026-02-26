package com.cjrequena.sample.command.handler.controller;

import com.cjrequena.sample.command.handler.controller.dto.*;
import com.cjrequena.sample.command.handler.controller.exception.BadRequestException;
import com.cjrequena.sample.command.handler.controller.exception.ConflictException;
import com.cjrequena.sample.command.handler.controller.exception.NotImplementedException;
import com.cjrequena.sample.command.handler.domain.exception.CommandHandlerNotFoundException;
import com.cjrequena.sample.command.handler.domain.exception.PaxPriceException;
import com.cjrequena.sample.command.handler.domain.mapper.CommandMapper;
import com.cjrequena.sample.command.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.command.handler.service.command.CommandBusService;
import com.cjrequena.sample.command.handler.shared.common.Constant;
import com.cjrequena.sample.es.core.domain.exception.OptimisticConcurrencyException;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = BookingCommandController.ENDPOINT, headers = {BookingCommandController.ACCEPT_VERSION})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BookingCommandController {

  public static final String ENDPOINT = "/command-handler/api/bookings";
  public static final String ACCEPT_VERSION = "Accept-Version=" + Constant.VND_BOOKING_COMMAND_HANDLER_V1;

  private final CommandBusService commandBusService;
  private final CommandMapper commandMapper;

  @SneakyThrows
  @PostMapping(
    path = "/place",
    produces = {APPLICATION_JSON_VALUE}
  )
  public Mono<ResponseEntity<CommandResponseDTO>> place(@Valid @RequestBody PlaceBookingCommandDTO dto, ServerHttpRequest request) {
    try {
      Command command = commandMapper.toCommand(dto);
      Booking booking = (Booking)this.commandBusService.handle(command);
      CommandResponseDTO responseDTO = CommandResponseDTO
        .builder()
        .bookingId(booking.getBookingId())
        .status(booking.getStatus())
        .build();

      HttpHeaders headers = new HttpHeaders();
      headers.set(CACHE_CONTROL, "no store, private, max-age=0");
      headers.set("Booking-Id", command.getAggregateId().toString());

      return Mono.just(
        ResponseEntity
          .status(HttpStatus.CREATED)
          .headers(headers)
          .body(responseDTO)
      );

    } catch (OptimisticConcurrencyException ex) {
      throw new ConflictException(ex.getMessage());
    } catch (CommandHandlerNotFoundException ex) {
      throw new NotImplementedException(ex.getMessage());
    } catch (PaxPriceException ex){
      throw new BadRequestException(ex.getMessage());
    }
  }

  @SneakyThrows
  @PostMapping(
    path = "/create",
    produces = {APPLICATION_JSON_VALUE}
  )
  public Mono<ResponseEntity<CommandResponseDTO>> create(@Valid @RequestBody CreateBookingCommandDTO dto, ServerHttpRequest request) {
    try {
      Command command = commandMapper.toCommand(dto);
      Booking booking = (Booking)this.commandBusService.handle(command);
      CommandResponseDTO responseDTO = CommandResponseDTO
        .builder()
        .bookingId(booking.getBookingId())
        .status(booking.getStatus())
        .build();

      HttpHeaders headers = new HttpHeaders();
      headers.set(CACHE_CONTROL, "no store, private, max-age=0");
      headers.set("Booking-Id", command.getAggregateId().toString());

      return Mono.just(
        ResponseEntity
          .status(HttpStatus.CREATED)
          .headers(headers)
          .body(responseDTO)
      );

    } catch (OptimisticConcurrencyException ex) {
      throw new ConflictException(ex.getMessage());
    } catch (CommandHandlerNotFoundException ex) {
      throw new NotImplementedException(ex.getMessage());
    } catch (PaxPriceException ex){
      throw new BadRequestException(ex.getMessage());
    }
  }

  @SneakyThrows
  @PostMapping(
    path = "/{bookingId}/confirm",
    produces = {APPLICATION_JSON_VALUE}
  )
  public Mono<ResponseEntity<CommandResponseDTO>> confirm(@PathVariable UUID bookingId, ServerHttpRequest request) {
    try {
      ConfirmBookingCommandDTO dto = ConfirmBookingCommandDTO.builder().bookingId(bookingId).build();
      Command command = commandMapper.toCommand(dto);
      Booking booking = (Booking)this.commandBusService.handle(command);
      CommandResponseDTO responseDTO = CommandResponseDTO
        .builder()
        .bookingId(booking.getBookingId())
        .status(booking.getStatus())
        .build();

      HttpHeaders headers = new HttpHeaders();
      headers.set(CACHE_CONTROL, "no store, private, max-age=0");
      headers.set("Booking-Id", command.getAggregateId().toString());

      return Mono.just(
        ResponseEntity
          .status(HttpStatus.OK)
          .headers(headers)
          .body(responseDTO)
      );

    } catch (OptimisticConcurrencyException ex) {
      throw new ConflictException(ex.getMessage());
    } catch (CommandHandlerNotFoundException ex) {
      throw new NotImplementedException(ex.getMessage());
    } catch (PaxPriceException ex){
      throw new BadRequestException(ex.getMessage());
    }
  }

  @SneakyThrows
  @PostMapping(
    path = "/{bookingId}/cancel",
    produces = {APPLICATION_JSON_VALUE}
  )
  public Mono<ResponseEntity<CommandResponseDTO>> cancel(@PathVariable UUID bookingId, ServerHttpRequest request) {
    try {
      CancelBookingCommandDTO dto = CancelBookingCommandDTO.builder().bookingId(bookingId).build();
      Command command = commandMapper.toCommand(dto);
      Booking booking = (Booking)this.commandBusService.handle(command);
      CommandResponseDTO responseDTO = CommandResponseDTO
        .builder()
        .bookingId(booking.getBookingId())
        .status(booking.getStatus())
        .build();

      HttpHeaders headers = new HttpHeaders();
      headers.set(CACHE_CONTROL, "no store, private, max-age=0");
      headers.set("Booking-Id", command.getAggregateId().toString());

      return Mono.just(
        ResponseEntity
          .status(HttpStatus.OK)
          .headers(headers)
          .body(responseDTO)
      );

    } catch (OptimisticConcurrencyException ex) {
      throw new ConflictException(ex.getMessage());
    } catch (CommandHandlerNotFoundException ex) {
      throw new NotImplementedException(ex.getMessage());
    } catch (PaxPriceException ex){
      throw new BadRequestException(ex.getMessage());
    }
  }

  @SneakyThrows
  @PostMapping(
    path = "/{bookingId}/complete",
    produces = {APPLICATION_JSON_VALUE}
  )
  public Mono<ResponseEntity<CommandResponseDTO>> complete(@PathVariable UUID bookingId, ServerHttpRequest request) {
    try {
      CompleteBookingCommandDTO dto = CompleteBookingCommandDTO.builder().bookingId(bookingId).build();
      Command command = commandMapper.toCommand(dto);
      Booking booking = (Booking)this.commandBusService.handle(command);
      CommandResponseDTO responseDTO = CommandResponseDTO
        .builder()
        .bookingId(booking.getBookingId())
        .status(booking.getStatus())
        .build();

      HttpHeaders headers = new HttpHeaders();
      headers.set(CACHE_CONTROL, "no store, private, max-age=0");
      headers.set("Booking-Id", command.getAggregateId().toString());

      return Mono.just(
        ResponseEntity
          .status(HttpStatus.OK)
          .headers(headers)
          .body(responseDTO)
      );

    } catch (OptimisticConcurrencyException ex) {
      throw new ConflictException(ex.getMessage());
    } catch (CommandHandlerNotFoundException ex) {
      throw new NotImplementedException(ex.getMessage());
    } catch (PaxPriceException ex){
      throw new BadRequestException(ex.getMessage());
    }
  }

  @SneakyThrows
  @PostMapping(
    path = "/{bookingId}/expire",
    produces = {APPLICATION_JSON_VALUE}
  )
  public Mono<ResponseEntity<CommandResponseDTO>> expire(@PathVariable UUID bookingId, ServerHttpRequest request) {
    try {
      ExpireBookingCommandDTO dto = ExpireBookingCommandDTO.builder().bookingId(bookingId).build();
      Command command = commandMapper.toCommand(dto);
      Booking booking = (Booking)this.commandBusService.handle(command);
      CommandResponseDTO responseDTO = CommandResponseDTO
        .builder()
        .bookingId(booking.getBookingId())
        .status(booking.getStatus())
        .build();

      HttpHeaders headers = new HttpHeaders();
      headers.set(CACHE_CONTROL, "no store, private, max-age=0");
      headers.set("Booking-Id", command.getAggregateId().toString());

      return Mono.just(
        ResponseEntity
          .status(HttpStatus.OK)
          .headers(headers)
          .body(responseDTO)
      );

    } catch (OptimisticConcurrencyException ex) {
      throw new ConflictException(ex.getMessage());
    } catch (CommandHandlerNotFoundException ex) {
      throw new NotImplementedException(ex.getMessage());
    } catch (PaxPriceException ex){
      throw new BadRequestException(ex.getMessage());
    }
  }
}
