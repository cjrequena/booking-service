package com.cjrequena.sample.query.handler.domain.model;

import java.io.Serializable;
import java.util.List;

/**
 * Immutable pagination envelope returned by query-side services.
 *
 * <p>Adapted from the standalone query handler's {@code PageResult}, but tailored for the
 * reactive MongoDB projection store. Field names serialize to snake_case via the module's
 * global Jackson configuration (e.g. {@code total_elements}, {@code total_pages}).</p>
 *
 * @param <T> the content element type
 * @author cjrequena
 */
public record PageResult<T>(
  List<T> content,
  long totalElements,
  int totalPages,
  int page,
  int size
) implements Serializable {

  /**
   * Builds a {@link PageResult}, deriving {@code totalPages} from {@code totalElements} and {@code size}.
   *
   * @param content       page content
   * @param totalElements total number of matching elements across all pages
   * @param page          zero-based page index
   * @param size          page size
   */
  public static <T> PageResult<T> of(List<T> content, long totalElements, int page, int size) {
    int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    return new PageResult<>(content, totalElements, totalPages, page, size);
  }
}
