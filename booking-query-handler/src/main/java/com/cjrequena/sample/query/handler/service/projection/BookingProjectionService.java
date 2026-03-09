package com.cjrequena.sample.query.handler.service.projection;

import com.cjrequena.sample.query.handler.domain.exception.BookingNotFoundException;
import com.cjrequena.sample.query.handler.domain.exception.BookingProjectionException;
import com.cjrequena.sample.query.handler.domain.mapper.PaxMapper;
import com.cjrequena.sample.query.handler.domain.mapper.ProductMapper;
import com.cjrequena.sample.query.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.BookingEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.repository.BookingProjectionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
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
 *   <li>save: Evicts cache before save to ensure consistency with reactive types</li>
 * </ul>
 * </p>
 * <p>
 * Note: @Transactional is not used as MongoDB reactive operations don't require
 * traditional transaction management. MongoDB provides atomic operations at the
 * document level by default.
 * </p>
 *
 * @author cjrequena
 */
@Service
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
   * 2. Saves to MongoDB (blocking operation)
   * 3. Manually evicts and updates cache
   * </p>
   * <p>
   * Cache Strategy:
   * - Evicts existing cache entry before save to prevent stale data
   * - Cache will be repopulated on next retrieveById call
   * </p>
   * <p>
   * Note: This method blocks to ensure the save completes before returning.
   * This is necessary when called from non-reactive contexts (e.g., event handlers).
   * </p>
   *
   * @param aggregate the booking aggregate to save
   * @return the saved booking entity
   * @throws BookingProjectionException if save fails
   */
  @CachePut(value = "bookings", key = "#aggregate.bookingId", cacheNames = "bookings", cacheManager = "redisCacheManager")
  public BookingEntity save(@Valid @NotNull Booking aggregate) {

    log.info("Saving Booking Aggregate to MongoDB ProjectionDB: bookingId={}", aggregate.getBookingId());

    // Validate aggregate has required data
    if (aggregate.getBookingId() == null) {
      throw new BookingProjectionException("Booking aggregate must have a bookingId");
    }

    // Build entity from aggregate
    BookingEntity booking = BookingEntity.builder()
      .bookingId(aggregate.getBookingId())
      .bookingReference(aggregate.getBookingReference())
      .status(aggregate.getStatus())
      .paxes(paxMapper.toPaxList(aggregate.getPaxes()))
      .leadPaxId(aggregate.getLeadPaxId())
      .products(productMapper.toProductList(aggregate.getProducts()))
      .metadata(aggregate.getMetadata())
      .build();

    // Evict cache before save to ensure consistency
    evictBookingCache(aggregate.getBookingId());

    try {
      // Block and wait for the save to complete
      BookingEntity savedEntity = bookingProjectionRepository
        .save(booking)
        .block();

      if (savedEntity == null) {
        throw new BookingProjectionException("Failed to save Booking to MongoDB ProjectionDB: save returned null");
      }

      log.info("Booking saved successfully to MongoDB ProjectionDB: bookingId={}", savedEntity.getBookingId());
      return savedEntity;

    } catch (Exception ex) {
      log.error("Failed to save Booking to MongoDB ProjectionDB: bookingId={}, error={}", 
        aggregate.getBookingId(), ex.getMessage(), ex);
      
      if (ex instanceof BookingProjectionException) {
        throw ex;
      }
      throw new BookingProjectionException(
        "Failed to save Booking to MongoDB ProjectionDB: " + ex.getMessage(),
        ex
      );
    }
  }

  /**
   * Saves a booking aggregate to the projection database reactively.
   * <p>
   * Use this method when working in a reactive context where you can properly
   * subscribe to the returned Mono.
   * </p>
   *
   * @param aggregate the booking aggregate to save
   * @return Mono containing the saved booking entity
   */
  @CachePut(value = "bookings", key = "#aggregate.bookingId", cacheNames = "bookings", cacheManager = "redisCacheManager")
  public Mono<BookingEntity> saveReactive(@Valid @NotNull Booking aggregate) {

    log.info("Saving Booking Aggregate to MongoDB ProjectionDB (reactive): bookingId={}", aggregate.getBookingId());

    // Validate aggregate has required data
    if (aggregate.getBookingId() == null) {
      return Mono.error(new BookingProjectionException("Booking aggregate must have a bookingId"));
    }

    // Build entity from aggregate
    BookingEntity booking = BookingEntity.builder()
      .bookingId(aggregate.getBookingId())
      .bookingReference(aggregate.getBookingReference())
      .status(aggregate.getStatus())
      .paxes(paxMapper.toPaxList(aggregate.getPaxes()))
      .leadPaxId(aggregate.getLeadPaxId())
      .products(productMapper.toProductList(aggregate.getProducts()))
      .metadata(aggregate.getMetadata())
      .build();

    // Evict cache before save to ensure consistency
    evictBookingCache(aggregate.getBookingId());

    return bookingProjectionRepository
      .save(booking)
      .doOnSuccess(saved ->
        log.info("Booking saved successfully to MongoDB ProjectionDB: bookingId={}", saved.getBookingId())
      )
      .doOnError(ex ->
        log.error("Failed to save Booking to MongoDB ProjectionDB: bookingId={}, error={}", 
          aggregate.getBookingId(), ex.getMessage(), ex)
      )
      .onErrorMap(ex -> {
        if (ex instanceof BookingProjectionException) {
          return ex;
        }
        return new BookingProjectionException(
          "Failed to save Booking to MongoDB ProjectionDB: " + ex.getMessage(),
          ex
        );
      });
  }

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
  @Cacheable(value = "bookings", key = "#bookingId", cacheNames = "bookings", cacheManager = "redisCacheManager")
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
  @CacheEvict(value = "bookings", key = "#bookingId", cacheNames = "bookings", cacheManager = "redisCacheManager")
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
  @CacheEvict(value = "bookings", allEntries = true, cacheNames = "bookings", cacheManager = "redisCacheManager")
  public void evictAllBookingsCache() {
    log.debug("Evicting all bookings from cache");
  }

}
