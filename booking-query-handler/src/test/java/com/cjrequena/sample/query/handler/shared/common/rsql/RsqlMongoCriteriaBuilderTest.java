package com.cjrequena.sample.query.handler.shared.common.rsql;

import com.cjrequena.sample.query.handler.domain.model.enums.BookingStatus;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RSQL to MongoDB Criteria")
class RsqlMongoCriteriaBuilderTest {

  private final RsqlMongoCriteriaBuilder builder = new RsqlMongoCriteriaBuilder(Map.of(
    "booking_id", FilterField.of("bookingId", UUID::fromString),
    "booking_reference", FilterField.text("bookingReference"),
    "status", FilterField.of("status", BookingStatus::valueOf)
  ));

  private Document build(String filter) {
    return builder.build(filter).getCriteriaObject();
  }

  @Test
  void blankFilterMatchesEverything() {
    assertThat(build(null)).isEmpty();
    assertThat(build("  ")).isEmpty();
  }

  @Test
  void equalityConvertsToTheFieldType() {
    assertThat(build("status==CONFIRMED")).isEqualTo(new Document("status", BookingStatus.CONFIRMED));
    UUID id = UUID.randomUUID();
    assertThat(build("booking_id==" + id)).isEqualTo(new Document("bookingId", id));
  }

  @Test
  void inAndNotEqual() {
    assertThat(build("status=in=(CONFIRMED,CANCELLED)"))
      .isEqualTo(new Document("status", new Document("$in", List.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED))));
    assertThat(build("status!=CANCELLED"))
      .isEqualTo(new Document("status", new Document("$ne", BookingStatus.CANCELLED)));
  }

  @Test
  void andOrCombine() {
    Document and = build("status==CONFIRMED;booking_reference==ABC");
    assertThat(and.getList("$and", Document.class)).containsExactly(
      new Document("status", BookingStatus.CONFIRMED), new Document("bookingReference", "ABC"));
    Document or = build("status==CONFIRMED,status==CANCELLED");
    assertThat(or.getList("$or", Document.class)).hasSize(2);
  }

  @Test
  void likeIsCaseInsensitiveLiteralSubstring() {
    Pattern pattern = (Pattern) build("booking_reference=like=a.b").get("bookingReference");
    assertThat(pattern.flags() & Pattern.CASE_INSENSITIVE).isNotZero();
    assertThat(pattern.matcher("xxA.Byy").find()).isTrue();
    // The dot is literal, not a regex wildcard.
    assertThat(pattern.matcher("aXb").find()).isFalse();
  }

  @Test
  void equalityWithStarIsAnAnchoredCaseInsensitiveWildcard() {
    Pattern pattern = (Pattern) build("booking_reference==BK*").get("bookingReference");
    assertThat(pattern.flags() & Pattern.CASE_INSENSITIVE).isNotZero();
    assertThat(pattern.matcher("bk-123").matches()).isTrue();
    assertThat(pattern.matcher("XBK-123").matches()).isFalse();
    // Only * is special; a dot stays literal.
    assertThat(((Pattern) build("booking_reference==a.*").get("bookingReference")).matcher("aXz").matches()).isFalse();
  }

  @Test
  void starIsLiteralOnNonTextFields() {
    assertThatThrownBy(() -> build("status==CONF*")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsFieldsOutsideTheAllowlist() {
    assertThatThrownBy(() -> build("paxes.email==x@example.com"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("paxes.email")
      .hasMessageNotContaining("x@example.com");
  }

  @Test
  void rejectsLikeOnNonTextFields() {
    assertThatThrownBy(() -> build("status=like=CONF")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsUnconvertibleValuesWithoutEchoingThem() {
    assertThatThrownBy(() -> build("booking_id==not-a-uuid"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("booking_id")
      .hasMessageNotContaining("not-a-uuid");
  }

  @Test
  void rejectsMalformedExpressions() {
    assertThatThrownBy(() -> build("status==")).isInstanceOf(IllegalArgumentException.class);
  }
}
