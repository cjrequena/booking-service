package com.cjrequena.sample.query.handler.shared.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortUtilsTest {

  private static final Set<String> SORTABLE = Set.of("status", "booking_reference");
  private static final Sort DEFAULT = Sort.by("status");

  @Test
  void blankExpressionIsUnsortedOrDefault() {
    assertThat(SortUtils.parseSort(null).isUnsorted()).isTrue();
    assertThat(SortUtils.parseSort(" ").isUnsorted()).isTrue();
    assertThat(SortUtils.parse(null, SORTABLE, DEFAULT)).isEqualTo(DEFAULT);
  }

  @Test
  void parsesSeveralFields() {
    Sort sort = SortUtils.parse("status,asc;booking_reference,DESC", SORTABLE, DEFAULT);
    assertThat(sort.getOrderFor("status").getDirection()).isEqualTo(Sort.Direction.ASC);
    assertThat(sort.getOrderFor("booking_reference").getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void rejectsPropertiesOutsideTheAllowlist() {
    assertThatThrownBy(() -> SortUtils.parse("paxes,asc", SORTABLE, DEFAULT))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("paxes");
  }

  @Test
  void rejectsMalformedExpressions() {
    assertThatThrownBy(() -> SortUtils.parseSort("status")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SortUtils.parseSort("status,up")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SortUtils.parseSort(" ,asc")).isInstanceOf(IllegalArgumentException.class);
  }
}
