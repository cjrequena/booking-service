package com.cjrequena.sample.command.handler.domain.model.command;

import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Command to expire a booking in the system.
 * <p>
 * Business Rules:
 * <ul>
 *   <li>Booking must exist</li>
 *   <li>Booking payment or confirmation deadline has passed</li>
 *   <li>Booking is not yet confirmed or paid</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Getter
@ToString(callSuper = true)
public class ExpireBookingCommand extends Command {

  public ExpireBookingCommand(
    @NotNull(message = "Booking ID is required") UUID bookingId
  ) {
    super(bookingId, AggregateType.BOOKING_ORDER.getType());
  }
}
