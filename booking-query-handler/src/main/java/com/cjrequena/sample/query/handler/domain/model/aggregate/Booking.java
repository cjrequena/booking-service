package com.cjrequena.sample.query.handler.domain.model.aggregate;

import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.query.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.query.handler.domain.model.enums.BookingStatus;
import com.cjrequena.sample.query.handler.domain.model.event.*;
import com.cjrequena.sample.query.handler.domain.model.vo.PaxVO;
import com.cjrequena.sample.query.handler.domain.model.vo.ProductVO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

import java.util.List;
import java.util.Map;
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

  @Valid
  Map<String, Object> metadata;

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
  public void applyEvent(BookingCreatedEvent event) {
    this.bookingId = event.getData().bookingId();
    this.bookingReference = event.getData().bookingReference();
    this.status = event.getData().status();
    this.paxes = event.getData().paxes();
    this.leadPaxId = event.getData().leadPaxId();
    this.products = event.getData().products();
    this.metadata = event.getData().metadata();
  }

  //==========================================================
  // Confirm Booking
  //==========================================================
  public void applyEvent(BookingConfirmedEvent event) {
    this.status = event.getData().status();
  }

  //==========================================================
  // Cancel Booking
  //==========================================================
  public void applyEvent(BookingCancelledEvent event) {
    this.status = event.getData().status();
  }

  //==========================================================
  // Complete Booking
  //==========================================================
  public void applyEvent(BookingCompletedEvent event) {
    this.status = event.getData().status();
  }

  //==========================================================
  // Expire Booking
  //==========================================================
  public void applyEvent(BookingExpiredEvent event) {
    this.status = event.getData().status();
  }

  @Nonnull
  @Override
  public String getAggregateType() {
    return AggregateType.BOOKING_ORDER.getType();
  }
}
