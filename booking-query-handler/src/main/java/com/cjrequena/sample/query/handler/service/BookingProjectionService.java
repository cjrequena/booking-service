package com.cjrequena.sample.query.handler.service;

import com.cjrequena.sample.query.handler.domain.exception.BookingNotFoundException;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.BookingEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.repository.BookingProjectionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service for managing booking query projections.
 * <p>
 * This service provides cached access to booking projections stored in MongoDB.
 * Caching strategy:
 * <ul>
 *   <li>retrieveById: Cached by booking ID with 10-minute TTL</li>
 *   <li>retrieve: Not cached (list operations typically shouldn't be cached)</li>
 * </ul>
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

  /**
   * Retrieves a booking by ID with caching.
   * <p>
   * Cache key: bookingId
   * Cache name: bookings
   * TTL: 10 minutes (configured in CacheConfiguration)
   * </p>
   *
   * @param bookingId the booking ID to retrieve
   * @return Mono containing the booking entity
   * @throws BookingNotFoundException if booking not found
   */
  @Cacheable(value = "bookings", key = "#bookingId", cacheManager = "caffeineCacheManager")
  public Mono<BookingEntity> retrieveById(UUID bookingId) {
    log.debug("Retrieving booking from database (cache miss): {}", bookingId);
    return bookingProjectionRepository
      .findById(bookingId)
      .switchIfEmpty(Mono.error(new BookingNotFoundException("BookingOrder not found by bookingId: " + bookingId)));
  }

  /**
   * Retrieves all bookings.
   * <p>
   * Note: This method is not cached as list operations can be expensive to cache
   * and may return stale data. Consider implementing pagination and caching
   * individual pages if needed.
   * </p>
   *
   * @return Flux of all booking entities
   */
  public Flux<BookingEntity> retrieve() {
    log.debug("Retrieving all bookings from database");
    return this.bookingProjectionRepository.findAll();
  }

  /**
   * Evicts a booking from cache.
   * <p>
   * Call this method when a booking is updated or deleted to ensure
   * cache consistency.
   * </p>
   *
   * @param bookingId the booking ID to evict from cache
   */
  @CacheEvict(value = "bookings", key = "#bookingId", cacheManager = "caffeineCacheManager")
  public void evictBookingCache(UUID bookingId) {
    log.debug("Evicting booking from cache: {}", bookingId);
  }

  /**
   * Evicts all bookings from cache.
   * <p>
   * Use this method sparingly, typically only for administrative operations
   * or when bulk updates occur.
   * </p>
   */
  @CacheEvict(value = "bookings", allEntries = true, cacheManager = "caffeineCacheManager")
  public void evictAllBookingsCache() {
    log.debug("Evicting all bookings from cache");
  }
}
