package com.cjrequena.sample.query.handler.service.projection;

import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.query.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.query.handler.domain.model.enums.AggregateType;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingProjectionHandler implements ProjectionHandler {

    private final BookingProjectionService bookingProjectionService;

    @Override
    public void handle(Aggregate aggregate) {
        log.debug("Saving or Updating read model for booking order aggregate {}", aggregate);

        // Get the current booking order
        final Booking booking = ((Booking) aggregate);

        // Save the current booking order in the ProjectionDB
        this.bookingProjectionService.save(booking);
    }


    @Nonnull
    @Override
    public AggregateType getAggregateType() {
        return AggregateType.BOOKING_ORDER;
    }
}
