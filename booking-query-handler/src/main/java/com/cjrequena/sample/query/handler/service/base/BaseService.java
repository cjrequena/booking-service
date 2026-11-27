package com.cjrequena.sample.query.handler.service.base;

import com.cjrequena.sample.query.handler.domain.model.PageResult;
import com.cjrequena.sample.query.handler.shared.common.rsql.FilterField;
import com.cjrequena.sample.query.handler.shared.common.rsql.RsqlMongoCriteriaBuilder;
import com.cjrequena.sample.query.handler.shared.common.util.SortUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;

/**
 * Filtering, sorting and offset/limit paging for query-side services backed by the MongoDB
 * projection store: the MongoDB counterpart of the standalone query handler's JPA/RSQL
 * {@code BaseService}. Subclasses declare what may be filtered and sorted.
 *
 * <p>Filters are RSQL (see {@link RsqlMongoCriteriaBuilder}), e.g.
 * {@code status==CONFIRMED;booking_reference==BK*}. Selectors use the API's snake_case names and
 * are mapped to entity properties, so the template's {@code QueryMapper} handles stored field
 * names and value types.</p>
 *
 * <p>Invalid input (malformed filter, unknown field or sort property, bad paging) raises
 * {@link IllegalArgumentException}, which the global handler turns into a 400.</p>
 *
 * @param <E> the document type
 */
@Log4j2
public abstract class BaseService<E> {

  public static final int MAX_LIMIT = 200;

  private RsqlMongoCriteriaBuilder criteriaBuilder;

  protected abstract MongoTemplate getMongoTemplate();

  protected abstract Class<E> getEntityClass();

  /** API selector name (snake_case) → filterable field. */
  protected abstract Map<String, FilterField> getFilterableFields();

  /** API sort name (snake_case) → entity property. */
  protected abstract Map<String, String> getSortableFields();

  protected Sort getDefaultSort() {
    return Sort.unsorted();
  }

  /** All matching entities, filtered and sorted, without paging. */
  protected List<E> findAllWithFiltersAndSort(String filter, String sort) {
    Query query = buildQuery(filter, sort);
    List<E> content = getMongoTemplate().find(query, getEntityClass());
    log.debug("Found {} {} [filter={}, sort={}]", content.size(), getEntityClass().getSimpleName(), filter, sort);
    return content;
  }

  /**
   * One page of matching entities plus pagination metadata.
   *
   * @param offset index of the first result; the reported page number is {@code offset / limit}
   * @param limit  page size, 1..{@value #MAX_LIMIT}
   */
  protected PageResult<E> findPageWithFiltersAndSort(String filter, int offset, int limit, String sort) {
    validatePagination(offset, limit);
    Query query = buildQuery(filter, sort);
    long total = getMongoTemplate().count(Query.of(query).limit(0).skip(0), getEntityClass());
    List<E> content = getMongoTemplate().find(query.skip(offset).limit(limit), getEntityClass());
    log.debug("Found {} of {} {} [filter={}, offset={}, limit={}, sort={}]",
      content.size(), total, getEntityClass().getSimpleName(), filter, offset, limit, sort);
    return PageResult.of(content, total, offset / limit, limit);
  }

  private Query buildQuery(String filter, String sort) {
    return new Query(criteriaBuilder().build(filter)).with(resolveSort(sort));
  }

  private Sort resolveSort(String sortExpression) {
    Sort requested = SortUtils.parse(sortExpression, getSortableFields().keySet(), getDefaultSort());
    return Sort.by(requested.stream()
      .map(order -> new Sort.Order(order.getDirection(), getSortableFields().getOrDefault(order.getProperty(), order.getProperty())))
      .toList());
  }

  private static void validatePagination(int offset, int limit) {
    if (offset < 0) {
      throw new IllegalArgumentException("offset must be >= 0");
    }
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new IllegalArgumentException("limit must be between 1 and %d".formatted(MAX_LIMIT));
    }
  }

  private RsqlMongoCriteriaBuilder criteriaBuilder() {
    if (criteriaBuilder == null) {
      criteriaBuilder = new RsqlMongoCriteriaBuilder(getFilterableFields());
    }
    return criteriaBuilder;
  }
}
