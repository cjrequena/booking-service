package com.cjrequena.sample.command.handler.domain.model.aggregate;

import com.cjrequena.sample.command.handler.domain.model.command.PlaceBookingCommand;
import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.command.handler.domain.model.enums.BookingStatus;
import com.cjrequena.sample.command.handler.domain.model.event.BookingPlacedEvent;
import com.cjrequena.sample.command.handler.domain.model.vo.BookingPlacedEventDataVO;
import com.cjrequena.sample.command.handler.domain.model.vo.PaxVO;
import com.cjrequena.sample.command.handler.domain.model.vo.ProductVO;
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
//   this.bookingOrderVO = BookingOrderVO
//      .builder()
//      .bookingId(event.getData().bookingId())
//      .bookingReference(event.getData().bookingReference())
//      .status(event.getData().status())
//      .paxes(event.getData().paxes())
//      .leadPaxId(event.getData().leadPaxId())
//      .products(event.getData().products())
//      .build();

    this.bookingId = event.getData().bookingId();
    this.bookingReference = event.getData().bookingReference();
    this.status = event.getData().status();
    this.paxes = event.getData().paxes();
    this.leadPaxId = event.getData().leadPaxId();
    this.products = event.getData().products();
  }

//  public void applyCommand(PlaceBookingOrderCommand command) throws JsonProcessingException {
//
//    EventExtension eventExtension = EventExtension.builder().build(); // TODO implement EventExtension
//
//    applyUnconfirmedEvent(BookingOrderPlacedEvent.builder()
//      .aggregateId(command.getAggregateId())
//      .aggregateVersion(getNextAggregateVersion())
//      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
//      .data(command.getBookingOrderVO())
//      .dataBase64(JsonUtil.objectToJsonBase64(command.getBookingOrderVO()))
//      .extension(eventExtension)
//      .build());
//  }
//
//  public void applyCommand(CancelBookingOrderCommand command) throws JsonProcessingException {
//    EventExtension eventExtension = EventExtension.builder().build(); // TODO implement EventExtension
//    applyUnconfirmedEvent(BookingOrderCancelledEvent.builder()
//      .aggregateId(command.getAggregateId())
//      .aggregateVersion(getNextAggregateVersion())
//      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
//      .data(command.getBookingOrderVO())
//      .dataBase64(JsonUtil.objectToJsonBase64(command.getBookingOrderVO()))
//      .extension(eventExtension)
//      .build());
//  }
//
//
//  public void applyCommand(CancelBookingOrderProductCommand command) throws JsonProcessingException {
//    EventExtension eventExtension = EventExtension.builder().build(); // TODO implement EventExtension
//    applyUnconfirmedEvent(BookingOrderProductCancelledEvent.builder()
//      .aggregateId(command.getAggregateId())
//      .aggregateVersion(getNextAggregateVersion())
//      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
//      .data(command.getCancelBookingOrderProductVO())
//      .dataBase64(JsonUtil.objectToJsonBase64(command.getCancelBookingOrderProductVO()))
//      .extension(eventExtension)
//      .build());
//  }
//
//  public void applyCommand(CompleteBookingOrderCommand command) throws JsonProcessingException {
//    EventExtension eventExtension = EventExtension.builder().build(); // TODO implement EventExtension
//    applyUnconfirmedEvent(BookingOrderCompletedEvent.builder()
//      .aggregateId(command.getAggregateId())
//      .aggregateVersion(getNextAggregateVersion())
//      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
//      .data(command.getBookingOrderVO())
//      .dataBase64(JsonUtil.objectToJsonBase64(command.getBookingOrderVO()))
//      .extension(eventExtension)
//      .build());
//  }
//
//  public void applyCommand(AcceptBookingOrderCommand command) throws JsonProcessingException {
//    EventExtension eventExtension = EventExtension.builder().build(); // TODO implement EventExtension
//    applyUnconfirmedEvent(BookingOrderAcceptedEvent.builder()
//      .aggregateId(command.getAggregateId())
//      .aggregateVersion(getNextAggregateVersion())
//      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
//      .data(command.getBookingOrderVO())
//      .dataBase64(JsonUtil.objectToJsonBase64(command.getBookingOrderVO()))
//      .extension(eventExtension)
//      .build());
//  }
//
//
//  public void applyCommand(InitiateBookingOrderPaymentCommand command) throws JsonProcessingException {
//    EventExtension eventExtension = EventExtension.builder().build(); // TODO implement EventExtension
//    applyUnconfirmedEvent(BookingOrderPaymentInitiatedEvent.builder()
//      .aggregateId(command.getAggregateId())
//      .aggregateVersion(getNextAggregateVersion())
//      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
//      .data(command.getBookingOrderVO())
//      .dataBase64(JsonUtil.objectToJsonBase64(command.getBookingOrderVO()))
//      .extension(eventExtension)
//      .build());
//  }
//
//  public void applyCommand(ConfirmBookingOrderPaymentCommand command) throws JsonProcessingException {
//    EventExtension eventExtension = EventExtension.builder().build(); // TODO implement EventExtension
//    applyUnconfirmedEvent(BookingOrderPaymentConfirmedEvent.builder()
//      .aggregateId(command.getAggregateId())
//      .aggregateVersion(getNextAggregateVersion())
//      .dataContentType(MediaType.APPLICATION_JSON_VALUE)
//      .data(command.getBookingOrderVO())
//      .dataBase64(JsonUtil.objectToJsonBase64(command.getBookingOrderVO()))
//      .extension(eventExtension)
//      .build());
//  }
//
//
//  public void applyEvent(BookingOrderPlacedEvent event) {
//    this.bookingOrderVO = event.getData();
//  }
//
//  public void applyEvent(BookingOrderCancelledEvent event) {
//    this.bookingOrderVO = this.bookingOrderVO.cloneWithStatus(event.getData().status());
//    this.bookingOrderVO.products().forEach(product -> product.setStatus(ProductStatus.CANCELLED));
//  }
//
//
//  public void applyEvent(BookingOrderProductCancelledEvent event) {
//    // Extract product IDs to cancel into a Set for faster lookup
//    Set<UUID> idsToCancel = new HashSet<>(event
//      .getData()
//      .products());
//
//    // Update the status of matching products to CANCELLED
//    this.bookingOrderVO.products().stream()
//      .filter(product -> idsToCancel.contains(product.getProductId()))
//      .forEach(product -> product.setStatus(ProductStatus.CANCELLED));
//
//    boolean areAllProductsCancelled = this.bookingOrderVO.products().stream()
//      .allMatch(product -> product.getStatus() == ProductStatus.CANCELLED);
//
//    if (areAllProductsCancelled) {
//      this.bookingOrderVO.cloneWithStatus(BookingStatus.CANCELLED);
//    }
//  }
//
//  public void applyEvent(BookingOrderCompletedEvent event) {
//    this.bookingOrderVO = this.bookingOrderVO.cloneWithStatus(event.getData().status());
//    this.bookingOrderVO.products().forEach(product -> product.setStatus(ProductStatus.COMPLETED));
//  }
//
//  public void applyEvent(BookingOrderAcceptedEvent event) {
//    this.bookingOrderVO = this.bookingOrderVO.cloneWithStatus(event.getData().status());
//    this.bookingOrderVO.products().forEach(product -> product.setStatus(ProductStatus.COMPLETED));
//  }
//
//  public void applyEvent(BookingOrderPaymentInitiatedEvent event) {
//    this.bookingOrderVO = this.bookingOrderVO.cloneWithStatus(event.getData().status());
//  }
//
//  public void applyEvent(BookingOrderPaymentConfirmedEvent event) {
//    this.bookingOrderVO = this.bookingOrderVO.cloneWithStatus(event.getData().status());
//  }

  @Nonnull
  @Override
  public String getAggregateType() {
    return AggregateType.BOOKING_ORDER.getType();
  }
}
