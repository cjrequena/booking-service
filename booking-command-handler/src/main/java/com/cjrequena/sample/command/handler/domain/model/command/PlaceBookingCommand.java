package com.cjrequena.sample.command.handler.domain.model.command;

import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.command.handler.domain.model.vo.PaxVO;
import com.cjrequena.sample.command.handler.domain.model.vo.ProductVO;
import com.cjrequena.sample.command.handler.shared.common.util.BookingReferenceGenerator;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

/**
 * Command to place a new booking in the system.
 * <p>
 * This command represents the intention to create a new booking with
 * passengers and products. It will be processed by a command handler
 * which will validate the business rules and produce a BookingPlacedEvent.
 * </p>
 * <p>
 * Business Rules:
 * <ul>
 *   <li>At least one passenger must be provided</li>
 *   <li>At least one product must be included</li>
 *   <li>Lead passenger must be one of the booking passengers</li>
 *   <li>Booking reference must be unique and valid</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Getter
@ToString(callSuper = true)
public class PlaceBookingCommand extends Command {

  @NotBlank(message = "Booking reference is required and cannot be blank")
  @Size(min = 1, max = 50, message = "Booking reference must be between 1 and 50 characters")
  private final String bookingReference;

  @NotEmpty(message = "At least one passenger is required")
  @Valid
  private final List<PaxVO> paxes;

  @NotNull(message = "Lead passenger ID is required")
  private final UUID leadPaxId;

  @NotEmpty(message = "At least one product is required")
  @Valid
  private final List<ProductVO> products;

  /**
   * Constructs a new PlaceBookingCommand.
   *
   * @param paxes the list of passengers
   * @param leadPaxId the ID of the lead passenger
   * @param products the list of products in the booking
   */
  @Builder
  public PlaceBookingCommand(
    List<PaxVO> paxes,
    UUID leadPaxId,
    List<ProductVO> products
  ) {
    super(UUID.randomUUID(), AggregateType.BOOKING_ORDER.getType());
    this.bookingReference = generateBookingReference();
    this.paxes = paxes;
    this.leadPaxId = leadPaxId;
    this.products = products;
  }

  private static String generateBookingReference() {
    return BookingReferenceGenerator.generateBookingReference("ORD");
  }
}
