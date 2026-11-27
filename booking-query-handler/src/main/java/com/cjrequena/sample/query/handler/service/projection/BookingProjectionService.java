package com.cjrequena.sample.query.handler.service.projection;

import com.cjrequena.sample.query.handler.domain.exception.BookingNotFoundException;
import com.cjrequena.sample.query.handler.domain.exception.BookingProjectionException;
import com.cjrequena.sample.query.handler.domain.mapper.PaxMapper;
import com.cjrequena.sample.query.handler.domain.mapper.ProductMapper;
import com.cjrequena.sample.query.handler.domain.model.PageResult;
import com.cjrequena.sample.query.handler.domain.model.aggregate.Booking;
import com.cjrequena.sample.query.handler.domain.model.enums.BookingStatus;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.BookingEntity;
import com.cjrequena.sample.query.handler.persistence.mongodb.repository.BookingProjectionRepository;
import com.cjrequena.sample.query.handler.service.sse.BookingUpdateMessage;
import com.cjrequena.sample.query.handler.service.base.MongoBaseService;
import com.cjrequena.sample.query.handler.service.sse.BookingUpdatePublisher;
import com.cjrequena.sample.query.handler.shared.common.rsql.FilterField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing booking query projections.
 * <p>
 * This service provides cached access to booking projections stored in MongoDB.
 * Caching strategy:
 * <ul>
 *   <li>retrieveById: Cached by booking ID with 10-minute TTL</li>
 *   <li>retrieve: Not cached (list operations typically shouldn't be cached)</li>
 *   <li>save: Evicts cache before save to ensure consistency</li>
 * </ul>
 * </p>
 * <p>
 * Note: @Transactional is not used: MongoDB provides atomic operations at the
 * document level by default.
 * </p>
 *
 * @author cjrequena
 */
@Service
@Log4j2
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class BookingProjectionService extends MongoBaseService<BookingEntity> {

  private static final Map<String, FilterField> FILTERABLE = Map.of(
    "booking_id", FilterField.of("bookingId", UUID::fromString),
    "booking_reference", FilterField.text("bookingReference"),
    "status", FilterField.of("status", BookingStatus::valueOf),
    "lead_pax_id", FilterField.of("leadPaxId", UUID::fromString)
  );

  private static final Map<String, String> SORTABLE = Map.of(
    "booking_reference", "bookingReference",
    "status", "status"
  );

  private final BookingProjectionRepository bookingProjectionRepository;
  private final PaxMapper paxMapper;
  private final ProductMapper productMapper;
  private final BookingUpdatePublisher bookingUpdatePublisher;
  private final MongoTemplate mongoTemplate;

  /**
   * Saves a booking aggregate to the projection database and updates cache.
   * <p>
   * This method:
   * 1. Maps the domain aggregate to a persistence entity
   * 2. Saves to MongoDB
   * 3. Manually evicts and updates cache
   * </p>
   * <p>
   * Cache Strategy:
   * - Evicts existing cache entry before save to prevent stale data
   * - Cache will be repopulated on next retrieveById call
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
      BookingEntity savedEntity = bookingProjectionRepository.save(booking);

      log.info("Booking saved successfully to MongoDB ProjectionDB: bookingId={}", savedEntity.getBookingId());
      bookingUpdatePublisher.publish(new BookingUpdateMessage(savedEntity.getBookingId().toString(), String.valueOf(savedEntity.getStatus())));
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
   * Retrieves a booking by ID with caching.
   * <p>
   * Cache key: bookingId
   * Cache name: bookings
   * TTL: 10 minutes (configured in CacheConfiguration)
   * </p>
   *
   * @param bookingId the booking ID to retrieve
   * @return the booking entity
   * @throws BookingNotFoundException if booking not found
   */
  @Cacheable(value = "bookings", key = "#bookingId", cacheNames = "bookings", cacheManager = "redisCacheManager")
  public BookingEntity retrieveById(UUID bookingId) {
    log.debug("Retrieving booking from database (cache miss): {}", bookingId);
    return bookingProjectionRepository
      .findById(bookingId)
      .orElseThrow(() -> new BookingNotFoundException("BookingOrder not found by bookingId: " + bookingId));
  }

  /**
   * Retrieves bookings matching an optional RSQL filter, sorted and optionally paged.
   * <p>
   * Not cached: list results go stale as soon as any booking changes.
   * </p>
   *
   * @param filter RSQL expression over {@code booking_id, booking_reference, status, lead_pax_id}; null for all
   * @param offset index of the first result; paging applies when offset or limit is given
   * @param limit  page size, 1..{@value MongoBaseService#MAX_LIMIT}
   * @param sort   e.g. {@code status,asc;booking_reference,desc}
   * @throws IllegalArgumentException on an invalid filter, sort or paging parameter
   */
  public PageResult<BookingEntity> retrieve(String filter, Integer offset, Integer limit, String sort) {
    return findPage(filter, offset, limit, sort);
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

  @Override
  protected MongoTemplate getMongoTemplate() {
    return mongoTemplate;
  }

  @Override
  protected Class<BookingEntity> getEntityClass() {
    return BookingEntity.class;
  }

  @Override
  protected Map<String, FilterField> getFilterableFields() {
    return FILTERABLE;
  }

  @Override
  protected Map<String, String> getSortableFields() {
    return SORTABLE;
  }
}
