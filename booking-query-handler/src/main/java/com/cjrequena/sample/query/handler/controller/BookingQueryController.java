package com.cjrequena.sample.query.handler.controller;

import com.cjrequena.sample.query.handler.controller.exception.NotFoundException;
import com.cjrequena.sample.query.handler.domain.exception.BookingNotFoundException;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.BookingEntity;
import com.cjrequena.sample.query.handler.service.BookingProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.cjrequena.sample.query.handler.shared.common.Constant.VND_BOOKING_ORDER_QUERY_HANDLER_V1;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Log4j2
@RestController
@RequestMapping(value = BookingQueryController.ENDPOINT, headers = {BookingQueryController.ACCEPT_VERSION})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "Booking Query API", description = "Query operations for booking projections")
public class BookingQueryController {

  public static final String ENDPOINT = "/query-handler/api";
  public static final String ACCEPT_VERSION = "accept-version=" + VND_BOOKING_ORDER_QUERY_HANDLER_V1;
  
  private final BookingProjectionService bookingProjectionService;

  @Operation(
    summary = "Retrieve booking by ID",
    description = "Retrieves a single booking projection by its unique identifier. Results are cached for improved performance.",
    parameters = {
      @Parameter(
        name = "accept-version", 
        required = true, 
        in = ParameterIn.HEADER,
        schema = @Schema(name = "accept-version", type = "string", allowableValues = {VND_BOOKING_ORDER_QUERY_HANDLER_V1})
      ),
      @Parameter(
        name = "bookingId",
        description = "Unique identifier of the booking",
        required = true,
        example = "123e4567-e89b-12d3-a456-426614174000"
      )
    }
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200", 
        description = "Successfully retrieved booking",
        content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = BookingEntity.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid booking ID format"),
      @ApiResponse(responseCode = "404", description = "Booking not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing credentials"),
      @ApiResponse(responseCode = "408", description = "Request timeout"),
      @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded"),
      @ApiResponse(responseCode = "500", description = "Internal server error"),
      @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    }
  )
  @GetMapping(path = "/bookings/{bookingId}", produces = {APPLICATION_JSON_VALUE})
  public Mono<ResponseEntity<BookingEntity>> retrieveById(@PathVariable("bookingId") UUID bookingId) {
    log.debug("Retrieving booking by ID: {}", bookingId);
    
    return bookingProjectionService
      .retrieveById(bookingId)
      .map(booking -> ResponseEntity
        .ok()
        .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
        .body(booking))
      .onErrorMap(BookingNotFoundException.class, ex -> {
        log.warn("Booking not found: {}", bookingId);
        return new NotFoundException(ex.getMessage());
      })
      .doOnError(ex -> log.error("Error retrieving booking {}: {}", bookingId, ex.getMessage()));
  }


  @Operation(
    summary = "Retrieve all bookings",
    description = "Retrieves a list of all booking projections. Consider implementing pagination for production use with large datasets.",
    parameters = {
      @Parameter(
        name = "accept-version", 
        required = true, 
        in = ParameterIn.HEADER,
        schema = @Schema(name = "accept-version", type = "string", allowableValues = {VND_BOOKING_ORDER_QUERY_HANDLER_V1})
      )
    }
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200", 
        description = "Successfully retrieved bookings list",
        content = @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = BookingEntity.class))
      ),
      @ApiResponse(responseCode = "400", description = "Bad request - Invalid query parameters"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing credentials"),
      @ApiResponse(responseCode = "408", description = "Request timeout"),
      @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded"),
      @ApiResponse(responseCode = "500", description = "Internal server error"),
      @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    }
  )
  @GetMapping(path = "/bookings", produces = {APPLICATION_JSON_VALUE})
  public Mono<ResponseEntity<Flux<BookingEntity>>> retrieve() {
    log.debug("Retrieving all bookings");
    
    Flux<BookingEntity> bookings = bookingProjectionService
      .retrieve()
      .doOnComplete(() -> log.debug("Successfully retrieved all bookings"))
      .doOnError(ex -> log.error("Error retrieving bookings: {}", ex.getMessage()));
    
    return Mono.just(ResponseEntity
      .ok()
      .cacheControl(CacheControl.noCache())
      .body(bookings));
  }

}
