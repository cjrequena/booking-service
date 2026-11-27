package com.cjrequena.sample.query.handler.shared.common.rsql;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.NoArgRSQLVisitorAdapter;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Translates an RSQL filter (e.g. {@code status==CONFIRMED;booking_reference=like=ORD}) into a
 * MongoDB {@link Criteria}.
 *
 * <p>Only the selectors in the {@code filterable} map are accepted. That is an allowlist rather
 * than a guard against specific fields: a field not listed (free text, personal data, anything
 * unindexed) cannot be searched at all. Selector names are the API's snake_case names; each maps
 * to an entity property and a converter, so UUIDs and enums are compared as their stored types.</p>
 *
 * <p>Operators: {@code ==}, {@code !=}, {@code =in=}, {@code =out=}, {@code =gt=}/{@code >},
 * {@code =ge=}/{@code >=}, {@code =lt=}/{@code <}, {@code =le=}/{@code <=}, and {@code =like=}
 * (case-insensitive literal substring, text fields only).</p>
 *
 * <p>Every rejection is an {@link IllegalArgumentException} naming the selector or operator, never
 * echoing a value, so callers map it to a 400.</p>
 */
public class RsqlMongoCriteriaBuilder {

  public static final ComparisonOperator LIKE = new ComparisonOperator("=like=");
  private static final RSQLParser PARSER;

  static {
    Set<ComparisonOperator> operators = new HashSet<>(RSQLOperators.defaultOperators());
    operators.add(LIKE);
    PARSER = new RSQLParser(operators);
  }

  private final Map<String, FilterField> filterable;

  public RsqlMongoCriteriaBuilder(Map<String, FilterField> filterable) {
    this.filterable = Map.copyOf(filterable);
  }

  /** @return the criteria, or an empty {@link Criteria} (matches everything) for a blank filter */
  public Criteria build(String filter) {
    if (filter == null || filter.isBlank()) {
      return new Criteria();
    }
    try {
      return PARSER.parse(filter).accept(new Visitor());
    } catch (RSQLParserException ex) {
      throw new IllegalArgumentException("Malformed filter expression", ex);
    }
  }

  private final class Visitor extends NoArgRSQLVisitorAdapter<Criteria> {

    @Override
    public Criteria visit(AndNode node) {
      return new Criteria().andOperator(node.getChildren().stream().map(child -> child.accept(this)).toList());
    }

    @Override
    public Criteria visit(OrNode node) {
      return new Criteria().orOperator(node.getChildren().stream().map(child -> child.accept(this)).toList());
    }

    @Override
    public Criteria visit(ComparisonNode node) {
      FilterField field = filterable.get(node.getSelector());
      if (field == null) {
        throw new IllegalArgumentException(
          "Field '%s' cannot be used in a filter. Filterable fields: %s".formatted(node.getSelector(), filterable.keySet()));
      }
      ComparisonOperator operator = node.getOperator();
      List<Object> values = node.getArguments().stream().map(argument -> convert(field, node.getSelector(), argument)).toList();
      Criteria criteria = Criteria.where(field.property());

      if (operator.equals(RSQLOperators.EQUAL)) {
        return criteria.is(values.getFirst());
      }
      if (operator.equals(RSQLOperators.NOT_EQUAL)) {
        return criteria.ne(values.getFirst());
      }
      if (operator.equals(RSQLOperators.IN)) {
        return criteria.in(values);
      }
      if (operator.equals(RSQLOperators.NOT_IN)) {
        return criteria.nin(values);
      }
      if (operator.equals(RSQLOperators.GREATER_THAN)) {
        return criteria.gt(values.getFirst());
      }
      if (operator.equals(RSQLOperators.GREATER_THAN_OR_EQUAL)) {
        return criteria.gte(values.getFirst());
      }
      if (operator.equals(RSQLOperators.LESS_THAN)) {
        return criteria.lt(values.getFirst());
      }
      if (operator.equals(RSQLOperators.LESS_THAN_OR_EQUAL)) {
        return criteria.lte(values.getFirst());
      }
      if (operator.equals(LIKE)) {
        if (!field.text()) {
          throw new IllegalArgumentException("Operator =like= is only supported on text fields, not '%s'".formatted(node.getSelector()));
        }
        return criteria.regex(Pattern.quote(String.valueOf(values.getFirst())), "i");
      }
      throw new IllegalArgumentException("Unsupported filter operator '%s'".formatted(operator.getSymbol()));
    }

    private Object convert(FilterField field, String selector, String argument) {
      try {
        return field.converter().apply(argument);
      } catch (RuntimeException ex) {
        throw new IllegalArgumentException("Invalid value for filter field '%s'".formatted(selector));
      }
    }
  }
}
