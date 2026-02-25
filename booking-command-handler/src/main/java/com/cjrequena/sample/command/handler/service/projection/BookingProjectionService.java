package com.cjrequena.sample.command.handler.service.projection;

import com.cjrequena.sample.command.handler.domain.exception.BookingProjectionException;
import com.cjrequena.sample.command.handler.domain.mapper.PaxMapper;
import com.cjrequena.sample.command.handler.domain.mapper.ProductMapper;
import com.cjrequena.sample.command.handler.persistence.mongodb.entity.BookingEntity;
import com.cjrequena.sample.command.handler.persistence.mongodb.repository.BookingProjectionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing booking command projections.
 * <p>
 * This service handles saving booking aggregates to the projection database
 * and manages cache updates to ensure consistency between command and query sides.
 * </p>
 *
 * @author cjrequena
 */
@Service
@Transactional
@Log4j2
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class BookingProjectionService {

  private final BookingProjectionRepository bookingProjectionRepository;
  private final PaxMapper paxMapper;
  private final ProductMapper productMapper;

  /**
   * Saves a booking aggregate to the projection database and updates cache.
   * <p>
   * This method:
   * 1. Maps the domain aggregate to a persistence entity
   * 2. Saves to MongoDB
   * 3. Updates the cache with the new entity
   * </p>
   * <p>
   * Cache Strategy:
   * - Uses @CachePut to update cache after save
   * - Cache key: bookingId
   * - Ensures query-side cache is always fresh after command execution
   * </p>
   *
   * @param aggregate the booking aggregate to save
   * @return the saved booking entity
   * @throws BookingProjectionException if save fails
   */
  @CachePut(value = "bookings", key = "#aggregate.bookingId")
  public BookingEntity save(@Valid @NotNull com.cjrequena.sample.command.handler.domain.model.aggregate.Booking aggregate) {
    log.info("Saving BookingOrder Aggregate to MongoDB ProjectionDB: {}", aggregate);
    try {

      final BookingEntity booking = BookingEntity
        .builder()
        .bookingId(aggregate.getBookingId())
        .bookingReference(aggregate.getBookingReference())
        .status(aggregate.getStatus())
        .paxes(paxMapper.toPaxList(aggregate.getPaxes()))
        .leadPaxId(aggregate.getLeadPaxId())
        .products(productMapper.toProductList(aggregate.getProducts()))
        .build();
      
      BookingEntity savedBooking = bookingProjectionRepository.save(booking);
      log.debug("Booking saved and cache updated: {}", savedBooking.getBookingId());
      return savedBooking;
    } catch (Exception ex) {
      throw new BookingProjectionException("Failed to save Booking Order to MongoDB ProjectionDB", ex);
    }
  }

}
