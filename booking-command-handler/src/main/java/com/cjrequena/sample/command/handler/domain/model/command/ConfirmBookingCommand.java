package com.cjrequena.sample.command.handler.domain.model.command;

import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Command to confirm a booking in the system.
 * <p>
 * This command represents the intention to confirm an existing booking.
 * It will be processed by a command handler which will validate the business
 * rules and produce a BookingConfirmedEvent.
 * </p>
 * <p>
 * Business Rules:
 * <ul>
 *   <li>Booking must exist</li>
 *   <li>Booking must be in CREATED or ACCEPTED status</li>
 *   <li>Booking cannot be already confirmed, cancelled, or expired</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Getter
@ToString(callSuper = true)
public class ConfirmBookingCommand extends Command {

  /**
   * Constructs a new ConfirmBookingCommand.
   *
   * @param bookingId the ID of the booking to confirm
   */
  public ConfirmBookingCommand(
    @NotNull(message = "Booking ID is required") UUID bookingId
  ) {
    super(bookingId, AggregateType.BOOKING_ORDER.getType());
  }
}
