package com.cjrequena.sample.query.handler.shared.common.rsql;

import java.util.function.Function;

/**
 * One field a caller may filter on.
 *
 * @param property  the entity property path the filter is applied to (Spring Data maps it to the stored field)
 * @param converter turns an RSQL argument into the property's type (e.g. {@code UUID::fromString})
 * @param text      whether {@code =like=} is allowed on it
 */
public record FilterField(String property, Function<String, Object> converter, boolean text) {

  public static FilterField text(String property) {
    return new FilterField(property, value -> value, true);
  }

  public static FilterField of(String property, Function<String, Object> converter) {
    return new FilterField(property, converter, false);
  }
}
