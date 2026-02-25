package com.cjrequena.sample.query.handler.controller;

import com.cjrequena.sample.query.handler.controller.exception.NotFoundException;
import com.cjrequena.sample.query.handler.domain.exception.BookingNotFoundException;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.BookingEntity;
import com.cjrequena.sample.query.handler.service.BookingProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.cjrequena.sample.query.handler.shared.common.Constant.VND_BOOKING_ORDER_QUERY_HANDLER_V1;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Log4j2
@RestController
@RequestMapping(value = BookingQueryController.ENDPOINT, headers = {BookingQueryController.ACCEPT_VERSION})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BookingQueryController {

  public static final String ENDPOINT = "/query-handler/api";
  public static final String ACCEPT_VERSION = "accept-version=" + VND_BOOKING_ORDER_QUERY_HANDLER_V1;
  private final BookingProjectionService bookingProjectionService;

  @Operation(
    summary = "Retrieve booking order by booking_id ",
    description = "Retrieve booking order by booking_id ",
    parameters = {
      @Parameter(name = "accept-version", required = true, in = ParameterIn.HEADER,
        schema = @Schema(name = "accept-version", type = "string", allowableValues = {VND_BOOKING_ORDER_QUERY_HANDLER_V1}))
    }
  )
  @ApiResponses(
    value = {
      @ApiResponse(responseCode = "200", description = "Created - The request was successful, we created a new resource and the response body contains the representation."),
      @ApiResponse(responseCode = "400", description = "Bad Request - The data given in the POST failed validation. Inspect the response body for details."),
      @ApiResponse(responseCode = "401", description = "Unauthorized - The supplied credentials, if any, are not sufficient to access the resource."),
      @ApiResponse(responseCode = "408", description = "Request Timeout"),
      @ApiResponse(responseCode = "409", description = "Conflict - The request could not be processed because of conflict in the request"),
      @ApiResponse(responseCode = "429", description = "Too Many Requests - Your application is sending too many simultaneous requests."),
      @ApiResponse(responseCode = "500", description = "Internal Server Error - We couldn't create the resource. Please try again."),
      @ApiResponse(responseCode = "503", description = "Service Unavailable - We are temporarily unable. Please wait for a bit and try again. ")
    }
  )
  @GetMapping(path = "/bookings/{bookingId}", produces = {APPLICATION_JSON_VALUE})
  public ResponseEntity<Mono<BookingEntity>> retrieveById(@PathVariable("bookingId") UUID bookingId) {
    Mono<BookingEntity> bookingOrderMono$ = this.bookingProjectionService
      .retrieveById(bookingId)
      .onErrorMap(BookingNotFoundException.class, ex -> new NotFoundException(ex.getMessage()));
    return ResponseEntity.ok().body(bookingOrderMono$);
  }


  @Operation(
    summary = "Get a list of bookings.",
    description = "Get a list of bookings.",
    parameters = {@Parameter(name = "accept-version", required = true, in = ParameterIn.HEADER,
      schema = @Schema(name = "accept-version", type = "string", allowableValues = {VND_BOOKING_ORDER_QUERY_HANDLER_V1}))}
  )
  @ApiResponses(
    value = {
      @ApiResponse(responseCode = "200", description = "OK - The request was successful and the response body contains the representation requested."),
      @ApiResponse(responseCode = "400", description = "Bad Request - The data given in the GET failed validation. Inspect the response body for details."),
      @ApiResponse(responseCode = "401", description = "Unauthorized - The supplied credentials, if any, are not sufficient to access the resource."),
      @ApiResponse(responseCode = "404", description = "Not Found"),
      @ApiResponse(responseCode = "408", description = "Request Timeout"),
      @ApiResponse(responseCode = "429", description = "Too Many Requests - Your application is sending too many simultaneous requests."),
      @ApiResponse(responseCode = "500", description = "Internal Server Error - We couldn't return the representation due to an internal server error."),
      @ApiResponse(responseCode = "503", description = "Service Unavailable - We are temporarily unable to return the representation. Please wait for a bit and try again."),
    }
  )
  @GetMapping(
    path = "/bookings",
    produces = {APPLICATION_JSON_VALUE}
  )
  public Mono<ResponseEntity<Flux<BookingEntity>>> retrieve() {
    final Flux<BookingEntity> bookingOrders$ = this.bookingProjectionService.retrieve();
    HttpHeaders responseHeaders = new HttpHeaders();
    return Mono.just(new ResponseEntity<>(bookingOrders$, HttpStatus.OK));
  }

}
