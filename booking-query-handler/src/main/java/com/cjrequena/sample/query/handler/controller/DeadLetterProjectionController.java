package com.cjrequena.sample.query.handler.controller;

import com.cjrequena.sample.query.handler.controller.exception.BadRequestException;
import com.cjrequena.sample.query.handler.controller.exception.ConflictException;
import com.cjrequena.sample.query.handler.controller.exception.NotFoundException;
import com.cjrequena.sample.query.handler.domain.exception.DeadLetterProjectionNotFoundException;
import com.cjrequena.sample.query.handler.domain.exception.InvalidDeadLetterProjectionStatusException;
import com.cjrequena.sample.query.handler.domain.model.PageResult;
import com.cjrequena.sample.query.handler.domain.model.enums.DeadLetterProjectionStatus;
import com.cjrequena.sample.query.handler.persistence.mongodb.entity.DeadLetterProjectionEntity;
import com.cjrequena.sample.query.handler.service.event.DeadLetterProjectionService;
import com.cjrequena.sample.query.handler.shared.common.util.SortUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

import static com.cjrequena.sample.query.handler.controller.BookingQueryController.ACCEPT_VERSION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Operator surface over projections that failed permanently and left an aggregate missing from,
 * or stale in, the read model.
 */
@RestController
@RequestMapping(value = DeadLetterProjectionController.ENDPOINT, headers = {ACCEPT_VERSION}, produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Dead Letter Projections", description = "Inspection and replay of projections that failed permanently")
public class DeadLetterProjectionController {

  public static final String ENDPOINT = BookingQueryController.ENDPOINT + "/dead-letter-projections";
  static final int DEFAULT_LIMIT = 20;
  static final int MAX_LIMIT = 200;
  /** An allowlist rather than a free expression: an arbitrary property would turn a typo into a 500. */
  static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "attempts", "lastAttemptAt", "status");
  static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

  private final DeadLetterProjectionService deadLetterProjectionService;

  @Operation(summary = "List failed projections", description = "Newest first unless sorted otherwise.")
  @GetMapping
  public ResponseEntity<PageResult<DeadLetterProjectionEntity>> retrieve(
    @Parameter(description = "Only projections in this state", example = "PENDING")
    @RequestParam(value = "status", required = false) DeadLetterProjectionStatus status,
    @Parameter(description = "Only projections of this aggregate type", example = "Booking")
    @RequestParam(value = "aggregate_type", required = false) String aggregateType,
    @RequestParam(value = "offset", defaultValue = "0") int offset,
    @RequestParam(value = "limit", defaultValue = "" + DEFAULT_LIMIT) int limit,
    @Parameter(description = "Sort expression, e.g. createdAt,desc", example = "createdAt,desc")
    @RequestParam(value = "sort", required = false) String sort
  ) throws BadRequestException {
    if (offset < 0 || limit < 1 || limit > MAX_LIMIT) {
      throw new BadRequestException("offset must be >= 0 and limit between 1 and %d".formatted(MAX_LIMIT));
    }
    Sort resolvedSort;
    try {
      resolvedSort = SortUtils.parse(sort, SORTABLE, DEFAULT_SORT);
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException(ex.getMessage());
    }
    return ResponseEntity.ok(deadLetterProjectionService.retrieve(status, aggregateType, offset, limit, resolvedSort));
  }

  @Operation(summary = "Get one failed projection")
  @GetMapping("/{id}")
  public ResponseEntity<DeadLetterProjectionEntity> retrieveById(@PathVariable UUID id) throws NotFoundException {
    try {
      return ResponseEntity.ok(deadLetterProjectionService.retrieveById(id));
    } catch (DeadLetterProjectionNotFoundException ex) {
      throw new NotFoundException(ex.getMessage());
    }
  }

  @Operation(summary = "Replay a failed projection",
    description = "Re-projects the aggregate from the event store. The response records whether it succeeded (REPLAYED) or failed again (FAILED).")
  @PostMapping("/{id}/replay")
  public ResponseEntity<DeadLetterProjectionEntity> replay(@PathVariable UUID id) throws NotFoundException, ConflictException {
    try {
      return ResponseEntity.ok(deadLetterProjectionService.replay(id));
    } catch (DeadLetterProjectionNotFoundException ex) {
      throw new NotFoundException(ex.getMessage());
    } catch (InvalidDeadLetterProjectionStatusException ex) {
      throw new ConflictException(ex.getMessage());
    }
  }

  @Operation(summary = "Discard a failed projection without replaying it")
  @PostMapping("/{id}/discard")
  public ResponseEntity<DeadLetterProjectionEntity> discard(@PathVariable UUID id) throws NotFoundException, ConflictException {
    try {
      return ResponseEntity.ok(deadLetterProjectionService.discard(id));
    } catch (DeadLetterProjectionNotFoundException ex) {
      throw new NotFoundException(ex.getMessage());
    } catch (InvalidDeadLetterProjectionStatusException ex) {
      throw new ConflictException(ex.getMessage());
    }
  }
}
