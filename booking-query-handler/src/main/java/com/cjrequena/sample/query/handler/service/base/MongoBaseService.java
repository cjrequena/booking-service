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
 * MongoDB counterpart of a JPA filter/sort/page base service: subclasses declare what may be
 * filtered and sorted, and get RSQL filtering, allowlisted sorting and offset/limit paging.
 *
 * <p>Paging is opt-in. With no {@code offset}/{@code limit} the whole (filtered, sorted) result is
 * returned, so existing list endpoints keep their behaviour.</p>
 *
 * <p>Invalid input (malformed filter, unknown field or sort property, bad paging) raises
 * {@link IllegalArgumentException}, which the global handler turns into a 400.</p>
 *
 * @param <E> the document type
 */
@Log4j2
public abstract class MongoBaseService<E> {

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

  protected PageResult<E> findPage(String filter, Integer offset, Integer limit, String sort) {
    Query query = new Query(criteriaBuilder().build(filter)).with(resolveSort(sort));
    boolean paged = offset != null || limit != null;
    int resolvedOffset = offset == null ? 0 : offset;
    int resolvedLimit = limit == null ? MAX_LIMIT : limit;
    if (paged) {
      validatePagination(resolvedOffset, resolvedLimit);
    }

    long total = getMongoTemplate().count(Query.of(query).limit(0).skip(0), getEntityClass());
    if (paged) {
      query.skip(resolvedOffset).limit(resolvedLimit);
    }
    List<E> content = getMongoTemplate().find(query, getEntityClass());
    log.debug("Found {} of {} {} [filter={}, offset={}, limit={}, sort={}]",
      content.size(), total, getEntityClass().getSimpleName(), filter, offset, limit, sort);
    return new PageResult<>(content, total, resolvedOffset, paged ? resolvedLimit : content.size());
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
