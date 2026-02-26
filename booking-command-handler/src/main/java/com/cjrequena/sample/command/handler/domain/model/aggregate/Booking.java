package com.cjrequena.sample.command.handler.domain.model.aggregate;

import com.cjrequena.sample.command.handler.domain.model.command.*;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.command.handler.domain.model.enums.BookingStatus;
import com.cjrequena.sample.command.handler.domain.model.event.*;
import com.cjrequena.sample.command.handler.domain.model.vo.*;
import com.cjrequena.sample.command.handler.shared.common.util.JsonUtil;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

@Getter
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Booking extends Aggregate {


  @NotNull(message = "Booking ID is required")
  private UUID bookingId;

  @NotBlank(message = "Booking reference is required and cannot be blank")
  @Size(min = 1, max = 50, message = "Booking reference must be between 1 and 50 characters")
  private String bookingReference;

  @NotNull(message = "Booking status is required")
  private BookingStatus status;

  @NotEmpty(message = "At least one passenger is required")
  @Valid
  private List<PaxVO> paxes;

  @NotNull(message = "Lead passenger ID is required")
  private UUID leadPaxId;

  @NotEmpty(message = "At least one product is required")
  @Valid
  private List<ProductVO> products;

  @Builder
  @JsonCreator
  public Booking(
    @NonNull @JsonProperty("aggregate_id") UUID aggregateId,
    @JsonProperty("aggregate_version") long aggregateVersion
  ) {
    super(aggregateId, aggregateVersion);
  }

  //==========================================================
  // Crate Booking
  //==========================================================
  public void applyCommand(CreateBookingCommand command) throws JsonProcessingException {
    final BookingCreatedEventDataVO data = BookingCreatedEventDataVO
      .builder()
      .bookingId(command.getAggregateId())
      .bookingReference(command.getBookingReference())
      .status(BookingStatus.CREATED)
      .paxes(command.getPaxes())
      .leadPaxId(command.getLeadPaxId())
      .products(command.getProducts())
      .build();

    applyUnconfirmedEvent(BookingCreatedEvent
      .builder()
      .eventId(java.util.UUID.randomUUID())
      .aggregateId(command.getAggregateId())
      .aggregateVersion(getNextAggregateVersion())
      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
      .data(data)
      .dataBase64(JsonUtil.objectToJsonBase64(data))
      .build());
  }

  public void applyEvent(BookingCreatedEvent event) {
    this.bookingId = event.getData().bookingId();
    this.bookingReference = event.getData().bookingReference();
    this.status = event.getData().status();
    this.paxes = event.getData().paxes();
    this.leadPaxId = event.getData().leadPaxId();
    this.products = event.getData().products();
  }

  //==========================================================
  // Place Booking
  //==========================================================
  
  public void applyCommand(PlaceBookingCommand command) throws JsonProcessingException {
    final BookingPlacedEventDataVO data = BookingPlacedEventDataVO
      .builder()
      .bookingId(command.getAggregateId())
      .bookingReference(command.getBookingReference())
      .status(BookingStatus.PLACED)
      .paxes(command.getPaxes())
      .leadPaxId(command.getLeadPaxId())
      .products(command.getProducts())
      .build();

    applyUnconfirmedEvent(BookingPlacedEvent
        .builder()
        .eventId(java.util.UUID.randomUUID())
        .aggregateId(command.getAggregateId())
        .aggregateVersion(getNextAggregateVersion())
        .dataContentType(MediaType.APPLICATION_JSON_VALUE)
        .data(data)
        .dataBase64(JsonUtil.objectToJsonBase64(data))
        .build());
  }

  public void applyEvent(BookingPlacedEvent event) {
    this.bookingId = event.getData().bookingId();
    this.bookingReference = event.getData().bookingReference();
    this.status = event.getData().status();
    this.paxes = event.getData().paxes();
    this.leadPaxId = event.getData().leadPaxId();
    this.products = event.getData().products();
  }

  //==========================================================
  // Confirm Booking
  //==========================================================

  public void applyCommand(ConfirmBookingCommand command) throws JsonProcessingException {
    final BookingConfirmedEventDataVO data = BookingConfirmedEventDataVO
      .builder()
      .bookingId(command.getAggregateId())
      .bookingReference(this.bookingReference)
      .status(BookingStatus.CONFIRMED)
      .build();

    applyUnconfirmedEvent(BookingConfirmedEvent
      .builder()
      .eventId(java.util.UUID.randomUUID())
      .aggregateId(command.getAggregateId())
      .aggregateVersion(getNextAggregateVersion())
      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
      .data(data)
      .dataBase64(JsonUtil.objectToJsonBase64(data))
      .build());
  }

  public void applyEvent(BookingConfirmedEvent event) {
    this.status = event.getData().status();
  }

  //==========================================================
  // Cancel Booking
  //==========================================================

  public void applyCommand(CancelBookingCommand command) throws JsonProcessingException {
    final BookingCancelledEventDataVO data = BookingCancelledEventDataVO
      .builder()
      .bookingId(command.getAggregateId())
      .bookingReference(this.bookingReference)
      .status(BookingStatus.CANCELLED)
      .build();

    applyUnconfirmedEvent(BookingCancelledEvent
      .builder()
      .eventId(java.util.UUID.randomUUID())
      .aggregateId(command.getAggregateId())
      .aggregateVersion(getNextAggregateVersion())
      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
      .data(data)
      .dataBase64(JsonUtil.objectToJsonBase64(data))
      .build());
  }

  public void applyEvent(BookingCancelledEvent event) {
    this.status = event.getData().status();
  }

  //==========================================================
  // Complete Booking
  //==========================================================

  public void applyCommand(CompleteBookingCommand command) throws JsonProcessingException {
    final BookingCompletedEventDataVO data = BookingCompletedEventDataVO
      .builder()
      .bookingId(command.getAggregateId())
      .bookingReference(this.bookingReference)
      .status(BookingStatus.COMPLETED)
      .build();

    applyUnconfirmedEvent(BookingCompletedEvent
      .builder()
      .eventId(java.util.UUID.randomUUID())
      .aggregateId(command.getAggregateId())
      .aggregateVersion(getNextAggregateVersion())
      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
      .data(data)
      .dataBase64(JsonUtil.objectToJsonBase64(data))
      .build());
  }

  public void applyEvent(BookingCompletedEvent event) {
    this.status = event.getData().status();
  }

  //==========================================================
  // Expire Booking
  //==========================================================

  public void applyCommand(ExpireBookingCommand command) throws JsonProcessingException {
    final BookingExpiredEventDataVO data = BookingExpiredEventDataVO
      .builder()
      .bookingId(command.getAggregateId())
      .bookingReference(this.bookingReference)
      .status(BookingStatus.EXPIRED)
      .build();

    applyUnconfirmedEvent(BookingExpiredEvent
      .builder()
      .eventId(java.util.UUID.randomUUID())
      .aggregateId(command.getAggregateId())
      .aggregateVersion(getNextAggregateVersion())
      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
      .data(data)
      .dataBase64(JsonUtil.objectToJsonBase64(data))
      .build());
  }

  public void applyEvent(BookingExpiredEvent event) {
    this.status = event.getData().status();
  }

  @Nonnull
  @Override
  public String getAggregateType() {
    return AggregateType.BOOKING_ORDER.getType();
  }
}
