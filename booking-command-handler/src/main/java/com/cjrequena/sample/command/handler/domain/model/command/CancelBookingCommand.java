package com.cjrequena.sample.command.handler.domain.model.command;

import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Command to cancel a booking in the system.
 * <p>
 * This command represents the intention to cancel an existing booking.
 * It will be processed by a command handler which will validate the business
 * rules and produce a BookingCancelledEvent.
 * </p>
 * <p>
 * Business Rules:
 * <ul>
 *   <li>Booking must exist</li>
 *   <li>Booking cannot be already cancelled or completed</li>
 *   <li>Cancellation may trigger refund processes</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Getter
@ToString(callSuper = true)
public class CancelBookingCommand extends Command {

  /**
   * Constructs a new CancelBookingCommand.
   *
   * @param bookingId the ID of the booking to cancel
   */
  public CancelBookingCommand(
    @NotNull(message = "Booking ID is required") UUID bookingId
  ) {
    super(bookingId, AggregateType.BOOKING_ORDER.getType());
  }
}
