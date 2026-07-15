package com.cjrequena.sample.query.handler.service.base;

import com.cjrequena.sample.query.handler.domain.model.PageResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base service providing common filtering, sorting, and offset/limit pagination for
 * query-side (read-only) services backed by a reactive MongoDB projection store.
 *
 * <p>This is the reactive counterpart of the standalone query handler's JPA/RSQL
 * {@code BaseService}. It cannot reuse {@code rsql-jpa} (JPA-only), so it builds
 * {@link Query}/{@link Criteria} against a {@link ReactiveMongoTemplate} instead. Filter
 * selectors are expressed as (and normalized to) entity property names — the template's
 * {@code QueryMapper} translates them to the stored {@code @Field} names.</p>
 *
 * <p>Subclasses must implement {@link #getMongoTemplate()} and {@link #getEntityClass()}.</p>
 *
 * <h3>Filter grammar (AND-only, RSQL-like)</h3>
 * <p>Semicolon-separated terms of the form {@code selector<op>value}, where {@code op} is one of
 * {@code ==}, {@code !=}, {@code >}, {@code >=}, {@code <}, {@code <=}. An {@code ==} value that
 * contains {@code *} is treated as a case-insensitive wildcard match. Values are coerced to
 * boolean/number when possible; quotes force a string. Example:
 * {@code status==CONFIRMED;booking_reference==BK*}.</p>
 *
 * @param <E> the entity type
 * @author cjrequena
 */
@Log4j2
public abstract class BaseService<E> {

  private static final Pattern TERM_PATTERN = Pattern.compile("^([\\w.]+)(==|!=|>=|<=|>|<)(.+)$");

  /** The reactive MongoDB template used to execute mapped queries. */
  protected abstract ReactiveMongoTemplate getMongoTemplate();

  /** The entity/document class, used for property-to-field mapping and collection resolution. */
  protected abstract Class<E> getEntityClass();

  // ================================================================
  // Query Methods
  // ================================================================

  /**
   * Finds entities with optional filtering, sorting, and offset/limit pagination.
   */
  protected Flux<E> findAllWithFiltersAndSort(String filter, Integer offset, Integer limit, String sort) {
    log.debug("Finding {} with filter: {}, offset: {}, limit: {}, sort: {}",
      getEntityClass().getSimpleName(), filter, offset, limit, sort);

    Query query = buildQuery(filter, sort);
    if (offset != null && limit != null) {
      validatePaginationParams(offset, limit);
      query.skip(offset).limit(limit);
    }
    return getMongoTemplate().find(query, getEntityClass());
  }

  /**
   * Finds a single page of entities together with pagination metadata.
   */
  protected Mono<PageResult<E>> findPageWithFiltersAndSort(String filter, Integer offset, Integer limit, String sort) {
    int effectiveOffset = offset != null ? offset : 0;
    int effectiveLimit = limit != null ? limit : 10;
    validatePaginationParams(effectiveOffset, effectiveLimit);

    log.debug("Finding page of {} with filter: {}, offset: {}, limit: {}, sort: {}",
      getEntityClass().getSimpleName(), filter, effectiveOffset, effectiveLimit, sort);

    Query contentQuery = buildQuery(filter, sort).skip(effectiveOffset).limit(effectiveLimit);
    Query countQuery = buildQuery(filter, null);

    Mono<List<E>> contentMono = getMongoTemplate().find(contentQuery, getEntityClass()).collectList();
    Mono<Long> countMono = getMongoTemplate().count(countQuery, getEntityClass());

    int page = effectiveLimit == 0 ? 0 : effectiveOffset / effectiveLimit;
    return Mono.zip(contentMono, countMono)
      .map(tuple -> PageResult.of(tuple.getT1(), tuple.getT2(), page, effectiveLimit));
  }

  /**
   * Counts entities matching the given filter.
   */
  protected Mono<Long> countWithFilters(String filter) {
    return getMongoTemplate().count(buildQuery(filter, null), getEntityClass());
  }

  // ================================================================
  // Private Helpers
  // ================================================================

  private Query buildQuery(String filter, String sort) {
    Query query = new Query();
    Criteria criteria = buildCriteria(filter);
    if (criteria != null) {
      query.addCriteria(criteria);
    }
    Sort sortObj = buildSort(sort);
    if (sortObj.isSorted()) {
      query.with(sortObj);
    }
    return query;
  }

  private Criteria buildCriteria(String filter) {
    if (filter == null || filter.isBlank()) {
      return null;
    }
    List<Criteria> andCriteria = new ArrayList<>();
    for (String rawTerm : filter.split(";")) {
      if (rawTerm.isBlank()) {
        continue;
      }
      Matcher matcher = TERM_PATTERN.matcher(rawTerm.trim());
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid filter term: " + rawTerm);
      }
      String field = snakeToCamel(matcher.group(1));
      String operator = matcher.group(2);
      String rawValue = matcher.group(3).trim();
      andCriteria.add(toCriteria(field, operator, rawValue));
    }
    if (andCriteria.isEmpty()) {
      return null;
    }
    if (andCriteria.size() == 1) {
      return andCriteria.get(0);
    }
    return new Criteria().andOperator(andCriteria.toArray(new Criteria[0]));
  }

  private Criteria toCriteria(String field, String operator, String rawValue) {
    Object value = coerceValue(rawValue);
    return switch (operator) {
      case "==" -> rawValue.contains("*")
        ? Criteria.where(field).regex("^" + Pattern.quote(rawValue).replace("*", "\\E.*\\Q") + "$", "i")
        : Criteria.where(field).is(value);
      case "!=" -> Criteria.where(field).ne(value);
      case ">" -> Criteria.where(field).gt(value);
      case ">=" -> Criteria.where(field).gte(value);
      case "<" -> Criteria.where(field).lt(value);
      case "<=" -> Criteria.where(field).lte(value);
      default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
    };
  }

  private Object coerceValue(String value) {
    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
      return value.substring(1, value.length() - 1);
    }
    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
      return Boolean.parseBoolean(value);
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      // not a long
    }
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException ignored) {
      // not a double
    }
    return value;
  }

  private Sort buildSort(String sort) {
    if (sort == null || sort.isBlank()) {
      return Sort.unsorted();
    }
    List<Sort.Order> orders = new ArrayList<>();
    for (String part : sort.split(";")) {
      if (part.isBlank()) {
        continue;
      }
      String[] tokens = part.trim().split("[,:]");
      String field = snakeToCamel(tokens[0].trim());
      Sort.Direction direction = tokens.length > 1 && "desc".equalsIgnoreCase(tokens[1].trim())
        ? Sort.Direction.DESC
        : Sort.Direction.ASC;
      orders.add(new Sort.Order(direction, field));
    }
    return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
  }

  private void validatePaginationParams(int offset, int limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException("Limit must be greater than 0");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Offset must be greater than or equal to 0");
    }
  }

  /**
   * Converts a dotted snake_case selector to camelCase, segment by segment, so that API callers
   * may use the snake_case field names seen in JSON while the template maps them to entity
   * properties. E.g. {@code paxes.lead_pax_id} → {@code paxes.leadPaxId}.
   */
  private String snakeToCamel(String selector) {
    if (!selector.contains("_")) {
      return selector;
    }
    String[] segments = selector.split("\\.");
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < segments.length; i++) {
      if (i > 0) {
        result.append('.');
      }
      result.append(segmentToCamel(segments[i]));
    }
    return result.toString();
  }

  private String segmentToCamel(String segment) {
    if (!segment.contains("_")) {
      return segment;
    }
    StringBuilder sb = new StringBuilder();
    boolean capitalizeNext = false;
    for (char c : segment.toCharArray()) {
      if (c == '_') {
        capitalizeNext = true;
      } else {
        sb.append(capitalizeNext ? Character.toUpperCase(c) : c);
        capitalizeNext = false;
      }
    }
    return sb.toString();
  }
}
