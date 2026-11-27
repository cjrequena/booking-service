package com.cjrequena.sample.query.handler.controller;

import com.cjrequena.sample.query.handler.controller.exception.NotFoundException;
import com.cjrequena.sample.query.handler.domain.exception.BookingNotFoundException;
import com.cjrequena.sample.query.handler.domain.model.PageResult;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.BookingEntity;
import com.cjrequena.sample.query.handler.service.projection.BookingProjectionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
  /** Total matches before paging; exposed to browsers by the CORS configuration. */
  public static final String TOTAL_COUNT_HEADER = "total-count";
  
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
  public ResponseEntity<BookingEntity> retrieveById(@PathVariable("bookingId") UUID bookingId) throws NotFoundException {
    log.debug("Retrieving booking by ID: {}", bookingId);
    try {
      BookingEntity booking = bookingProjectionService.retrieveById(bookingId);
      return ResponseEntity
        .ok()
        .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
        .body(booking);
    } catch (BookingNotFoundException ex) {
      log.warn("Booking not found: {}", bookingId);
      throw new NotFoundException(ex.getMessage());
    }
  }


  @Operation(
    summary = "Retrieve bookings",
    description = "Retrieves booking projections, optionally filtered (RSQL), sorted and paged. The total-count response header carries the number of matches before paging.",
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
  public ResponseEntity<List<BookingEntity>> retrieve(
    @Parameter(description = "RSQL filter over booking_id, booking_reference, status, lead_pax_id",
      example = "status==CONFIRMED;booking_reference=like=ORD")
    @RequestParam(value = "filter", required = false) String filter,
    @Parameter(description = "Index of the first result (0-based). Paging applies when offset or limit is given.", example = "0")
    @RequestParam(value = "offset", required = false) Integer offset,
    @Parameter(description = "Page size, 1 to 200", example = "20")
    @RequestParam(value = "limit", required = false) Integer limit,
    @Parameter(description = "Sort by booking_reference and/or status", example = "status,asc;booking_reference,desc")
    @RequestParam(value = "sort", required = false) String sort
  ) {
    log.debug("Retrieving bookings [filter={}, offset={}, limit={}, sort={}]", filter, offset, limit, sort);
    PageResult<BookingEntity> page = bookingProjectionService.retrieve(filter, offset, limit, sort);
    return ResponseEntity
      .ok()
      .cacheControl(CacheControl.noCache())
      .header(TOTAL_COUNT_HEADER, String.valueOf(page.totalElements()))
      .body(page.content());
  }

}
