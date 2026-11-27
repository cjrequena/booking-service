package com.cjrequena.sample.query.handler.service.event;

import com.cjrequena.sample.es.core.configuration.EventStoreConfigurationProperties;
import com.cjrequena.sample.es.core.domain.model.aggregate.Aggregate;
import com.cjrequena.sample.es.core.domain.model.event.Event;
import com.cjrequena.sample.es.core.persistence.entity.EventEntity;
import com.cjrequena.sample.es.core.service.EventStoreService;
import com.cjrequena.sample.query.handler.domain.mapper.EventMapper;
import com.cjrequena.sample.query.handler.domain.model.enums.AggregateType;
import com.cjrequena.sample.query.handler.service.projection.ProjectionHandler;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import jakarta.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Keeps the MongoDB read model in step with the event store, one aggregate at a time.
 *
 * <p>A failure is classified per aggregate, so one bad aggregate cannot stall the subscription:</p>
 * <ul>
 *   <li><b>Transient</b> (database or broker unreachable, timeouts): rethrown. The poll transaction
 *   rolls back, the subscription offset is not advanced, and the batch is retried.</li>
 *   <li><b>Anything else</b> (bad data, mapping bugs, constraint violations): will not succeed on
 *   retry, so the aggregate is dead-lettered via {@link DeadLetterService} and the offset advances.
 *   The failure stays visible and can be replayed through {@link #reproject(UUID)} once fixed.</li>
 * </ul>
 */
@Component
@Transactional
@Log4j2
public class BookingEventHandler extends EventHandler {

  private final List<ProjectionHandler> projectionHandlers;
  private final DeadLetterService deadLetterService;

  @Value("${projections.handlers.enabled}")
  private boolean projectionsHandlersEnabled;

  @Autowired
  public BookingEventHandler(
    EventStoreService eventStoreService,
    EventMapper eventMapper,
    EventStoreConfigurationProperties eventStoreConfigurationProperties,
    List<ProjectionHandler> projectionHandlers,
    DeadLetterService deadLetterService
  ) {
    super(eventStoreService, eventMapper, eventStoreConfigurationProperties);
    this.projectionHandlers = projectionHandlers;
    this.deadLetterService = deadLetterService;
  }

  @Override
  public void handle(List<EventEntity> eventEntityList) {
    if (!projectionsHandlersEnabled) {
      return;
    }
    final List<Event> events = this.eventMapper.toEventList(eventEntityList);
    events
      .parallelStream()
      .map(Event::getAggregateId)
      .distinct()
      .forEach(aggregateId -> {
        try {
          reproject(aggregateId);
        } catch (RuntimeException ex) {
          if (isTransient(ex)) {
            throw ex;
          }
          String cause = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
          log.error("Permanent failure projecting aggregate, dead-lettering [aggregateId={}, cause={}]", aggregateId, cause, ex);
          deadLetterService.record(aggregateId, getAggregateType().getType(), cause);
        }
      });
  }

  /**
   * Rebuilds one aggregate from the event store and runs every projection handler for its type.
   *
   * <p>The single path for both live delivery and dead-letter replay, so the two cannot drift
   * apart. Projections are upserts keyed by aggregate, so re-running one is safe.</p>
   */
  public void reproject(UUID aggregateId) {
    final Aggregate aggregate = retrieveOrInstantiateAggregate(aggregateId);
    projectionHandlers
      .stream()
      .filter(handler -> handler.getAggregateType().getType().equals(aggregate.getAggregateType()))
      .forEach(handler -> handler.handle(aggregate));
  }

  /** True if the cause chain shows infrastructure that may recover on its own, so a retry can succeed. */
  static boolean isTransient(Throwable ex) {
    for (Throwable t = ex; t != null; t = t.getCause()) {
      if (t instanceof TransientDataAccessException
        || t instanceof RecoverableDataAccessException
        || t instanceof DataAccessResourceFailureException
        || t instanceof CannotCreateTransactionException
        || t instanceof MongoSocketException
        || t instanceof MongoTimeoutException) {
        return true;
      }
      if (t.getCause() == t) {
        break;
      }
    }
    return false;
  }

  @Nonnull
  @Override
  public AggregateType getAggregateType() {
    return AggregateType.BOOKING_ORDER;
  }
}
