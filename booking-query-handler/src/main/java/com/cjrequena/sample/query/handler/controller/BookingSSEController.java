package com.cjrequena.sample.query.handler.controller;

import com.cjrequena.sample.query.handler.service.sse.BookingSSEService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static com.cjrequena.sample.query.handler.controller.BookingQueryController.ACCEPT_VERSION;
import static com.cjrequena.sample.query.handler.controller.BookingQueryController.ENDPOINT;

@RestController
@RequestMapping(value = ENDPOINT, headers = {ACCEPT_VERSION})
@RequiredArgsConstructor
@Tag(name = "Booking Query API", description = "Query operations for booking projections")
public class BookingSSEController {

  private final BookingSSEService bookingSSEService;

  @Operation(
    summary = "Stream live booking updates",
    description = "Server-Sent Events stream. Each 'booking-update' event carries booking_id and status; "
      + "clients refetch the booking to get its full projection."
  )
  @GetMapping(value = "/bookings/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(
    @Parameter(description = "Follow a single booking; omit to follow every booking")
    @RequestParam(value = "booking_id", required = false) UUID bookingId
  ) {
    return bookingSSEService.subscribe(bookingId);
  }
}
