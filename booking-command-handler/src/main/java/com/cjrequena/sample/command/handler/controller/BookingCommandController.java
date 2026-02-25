package com.cjrequena.sample.command.handler.controller;

import com.cjrequena.sample.command.handler.controller.dto.PlaceBookingCommandDTO;
import com.cjrequena.sample.command.handler.controller.exception.BadRequestException;
import com.cjrequena.sample.command.handler.controller.exception.ConflictException;
import com.cjrequena.sample.command.handler.controller.exception.NotImplementedException;
import com.cjrequena.sample.command.handler.domain.exception.CommandHandlerNotFoundException;
import com.cjrequena.sample.command.handler.domain.exception.PaxPriceException;
import com.cjrequena.sample.command.handler.domain.mapper.CommandMapper;
import com.cjrequena.sample.command.handler.domain.model.enums.BookingStatus;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
  public Mono<ResponseEntity<String>> place(@Valid @RequestBody PlaceBookingCommandDTO dto, ServerHttpRequest request) {
    try {
      Command command = commandMapper.toCommand(dto);
      this.commandBusService.handle(command);

      HttpHeaders headers = new HttpHeaders();
      headers.set(CACHE_CONTROL, "no store, private, max-age=0");
      headers.set("booking_id", command.getAggregateId().toString());

      return Mono.just(
        ResponseEntity
          .status(HttpStatus.CREATED)
          .headers(headers)
          .body(String.format("{ \"booking_id\": \"%s\",\"status\": \"%s\"}", command.getAggregateId(), BookingStatus.PLACED))
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
