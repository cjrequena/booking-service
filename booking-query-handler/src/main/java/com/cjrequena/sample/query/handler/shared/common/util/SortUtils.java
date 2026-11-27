package com.cjrequena.sample.query.handler.shared.common.util;

import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility for parsing sort expressions into Spring Data {@link Sort} objects.
 * <p>Supported formats:
 * <ul>
 *   <li>Single field: {@code "status,asc"}</li>
 *   <li>Multiple fields: {@code "status,asc;createdAt,desc"}</li>
 * </ul>
 *
 * @author cjrequena
 */
public final class SortUtils {

  private SortUtils() {
  }

  public static Sort parseSort(String sortExpression) {
    if (sortExpression == null || sortExpression.isBlank()) {
      return Sort.unsorted();
    }
    List<Sort.Order> orders = new ArrayList<>();
    for (String field : sortExpression.split(";")) {
      String[] parts = field.trim().split(",");
      if (parts.length != 2) {
        throw new IllegalArgumentException(
          "Invalid sort format: '%s'. Expected 'property,direction'".formatted(field));
      }
      String property = parts[0].trim();
      String direction = parts[1].trim().toLowerCase();
      if (property.isBlank()) {
        throw new IllegalArgumentException("Sort property cannot be empty");
      }
      Sort.Direction sortDirection = switch (direction) {
        case "asc" -> Sort.Direction.ASC;
        case "desc" -> Sort.Direction.DESC;
        default -> throw new IllegalArgumentException(
          "Invalid sort direction: '%s'. Must be 'asc' or 'desc'".formatted(direction));
      };
      orders.add(new Sort.Order(sortDirection, property));
    }
    return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
  }

  /**
   * Parses {@code sortExpression}, rejecting any property outside {@code sortable}.
   *
   * @return {@code defaultSort} when the expression is blank
   * @throws IllegalArgumentException on a malformed expression or a property not in the allowlist
   */
  public static Sort parse(String sortExpression, Set<String> sortable, Sort defaultSort) {
    Sort sort = parseSort(sortExpression);
    if (sort.isUnsorted()) {
      return defaultSort;
    }
    for (Sort.Order order : sort) {
      if (!sortable.contains(order.getProperty())) {
        throw new IllegalArgumentException(
          "Cannot sort by '%s'. Sortable properties: %s".formatted(order.getProperty(), sortable));
      }
    }
    return sort;
  }
}
