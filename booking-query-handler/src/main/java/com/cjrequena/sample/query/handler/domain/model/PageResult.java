package com.cjrequena.sample.query.handler.domain.model;

import java.util.List;

/**
 * Generic page result for paginated queries.
 *
 * @param content       the items in the current page
 * @param totalElements total number of items across all pages
 * @param offset        index of the first item of this page
 * @param limit         page size
 * @param <T>           the element type
 */
public record PageResult<T>(
  List<T> content,
  long totalElements,
  int offset,
  int limit
) {
}
