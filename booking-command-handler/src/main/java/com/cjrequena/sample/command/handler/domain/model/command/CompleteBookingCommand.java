package com.cjrequena.sample.command.handler.domain.model.command;

import com.cjrequena.sample.command.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.es.core.domain.model.command.Command;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Command to complete a booking in the system.
 * <p>
 * Business Rules:
 * <ul>
 *   <li>Booking must exist</li>
 *   <li>Booking must be in CONFIRMED status</li>
 *   <li>Service must have been delivered</li>
 * </ul>
 * </p>
 *
 * @author cjrequena
 */
@Getter
@ToString(callSuper = true)
public class CompleteBookingCommand extends Command {

  @Builder
  public CompleteBookingCommand(
    @NotNull(message = "Booking ID is required") UUID bookingId
  ) {
    super(bookingId, AggregateType.BOOKING_ORDER.getType());
  }
}
