package com.openrecordsmanager.search.sql;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.InputValidationException;
import com.openrecordsmanager.api.search.SearchMatchMode;
import com.openrecordsmanager.api.search.SearchOperator;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.search.SearchCriteriaExpander;
import com.openrecordsmanager.search.sql.dialect.JsonSearchDialect;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL-first ID lookup for {@code ObjectPropertyHolder} property criteria.
 */
@Component
public class SqlSearchSupport {

    private final JdbcTemplate jdbcTemplate;
    private final JsonSearchDialect dialect;

    public SqlSearchSupport(JdbcTemplate jdbcTemplate, JsonSearchDialect dialect) {
        this.jdbcTemplate = jdbcTemplate;
        this.dialect = dialect;
    }

    public JsonSearchDialect getDialect() {
        return this.dialect;
    }

    /**
     * Returns matching holder ids ordered by id ascending.
     *
     * @param afterId exclusive cursor (might be null)
     * @param limit   max ids to return
     */
    public List<UUID> findMatchingIds(
            ObjectSearchSchema schema,
            SearchCriteriaExpander.ExpandedCriteria criteria,
            SearchMatchMode matchMode,
            @Nullable ResourceIdentifier typeScope,
            @Nullable UUID afterId,
            int limit
    ) {
        List<@Nullable Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();

        if (typeScope != null) {
            if (schema.typeColumn() == null) {
                throw new InputValidationException(Map.of("type", "Type scope is not supported for this search target"));
            }
            where.add(schema.typeColumn() + " = ?");
            params.add(typeScope.toString());
        }
        if (afterId != null) {
            where.add(schema.idColumn() + " > ?");
            params.add(afterId);
        }

        List<String> groups = new ArrayList<>();

        if (!criteria.qClauses().isEmpty()) {
            String qQuery = criteria.qClauses().stream()
                    .map(clause -> clause.toSqlClause(this, schema, params))
                    .collect(Collectors.joining(" OR "));
            groups.add("(" + qQuery + ")");
        }

        if (!criteria.propertyFilters().isEmpty()) {
            String joiner = matchMode == SearchMatchMode.ANY ? " OR " : " AND ";

            String filterQuery = criteria.propertyFilters().stream()
                    .map(clause -> clause.toSqlClause(this, schema, params))
                    .collect(Collectors.joining(joiner));

            groups.add("(" + filterQuery + ")");
        }

        if (!groups.isEmpty()) {
            if (matchMode == SearchMatchMode.ANY && groups.size() > 1) {
                where.add("(" + String.join(" OR ", groups) + ")");
            } else {
                where.add(String.join(" AND ", groups));
            }
        }

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(schema.idColumn())
                .append(" FROM ")
                .append(this.dialect.quoteIdent(schema.tableName()));

        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }

        String finalSql = sql.append(" ORDER BY ")
                .append(schema.idColumn())
                .append(" ASC")
                .append(this.dialect.withLimit(Math.max(1, limit)))
                .toString();

        return this.jdbcTemplate.query(
                finalSql,
                ps -> this.bindParams(ps, params),
                (rs, _) -> toUuid(rs.getObject(1))
        );
    }

    private void bindParams(PreparedStatement ps, List<@Nullable Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            int index = i + 1;
            switch (param) {
                case null -> ps.setNull(index, Types.NULL);
                case UUID uuid -> ps.setObject(index, this.dialect.convertUuid(uuid));
                case Instant instant -> ps.setTimestamp(index, Timestamp.from(instant));
                case Boolean bool -> ps.setBoolean(index, bool);
                case Long number -> ps.setLong(index, number);
                case Double number -> ps.setDouble(index, number);
                case Integer number -> ps.setInt(index, number);
                default -> ps.setObject(index, param);
            }
        }
    }

    public String compare(
            String expr,
            PropertyType<?> type,
            SearchOperator op,
            @Nullable JsonNode value,
            List<@Nullable Object> params
    ) {
        return switch (op) {
            case IS_NULL -> expr + " IS NULL";
            case IS_NOT_NULL -> expr + " IS NOT NULL";
            case EQ -> {
                params.add(bindScalar(type, value));
                yield expr + " = ?";
            }
            case NEQ -> {
                params.add(bindScalar(type, value));
                yield expr + " <> ?";
            }
            case GT -> {
                params.add(bindScalar(type, value));
                yield expr + " > ?";
            }
            case GTE -> {
                params.add(bindScalar(type, value));
                yield expr + " >= ?";
            }
            case LT -> {
                params.add(bindScalar(type, value));
                yield expr + " < ?";
            }
            case LTE -> {
                params.add(bindScalar(type, value));
                yield expr + " <= ?";
            }
            case LIKE -> {
                if (value == null || value.isNull()) {
                    throw new InputValidationException(Map.of("value", "LIKE requires a string value"));
                }
                params.add(SearchWildcard.toLikePattern(value.asString()));
                yield this.dialect.like(expr, "?") + " ESCAPE '\\'";
            }
            case IN -> inList(expr, type, value, params, false);
            case NOT_IN -> inList(expr, type, value, params, true);
            case BETWEEN -> {
                if (value == null || !value.isArray() || value.size() != 2) {
                    throw new InputValidationException(Map.of("value", "BETWEEN requires two values"));
                }
                params.add(bindScalar(type, value.get(0)));
                params.add(bindScalar(type, value.get(1)));
                yield expr + " BETWEEN ? AND ?";
            }
        };
    }

    private String inList(
            String expr,
            PropertyType<?> type,
            @Nullable JsonNode value,
            List<@Nullable Object> params,
            boolean negate
    ) {
        if (value == null || !value.isArray() || value.isEmpty()) {
            throw new InputValidationException(Map.of("value", "IN requires a non-empty array"));
        }
        List<String> placeholders = new ArrayList<>(value.size());
        for (JsonNode element : value) {
            params.add(bindScalar(type, element));
            placeholders.add("?");
        }
        return expr + (negate ? " NOT IN (" : " IN (") + String.join(", ", placeholders) + ")";
    }

    private static @Nullable Object bindScalar(PropertyType<?> type, @Nullable JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (type == PropertyType.NUMBER) {
            return value.isNumber() ? value.asLong() : Long.parseLong(value.asString().trim());
        }
        if (type == PropertyType.DECIMAL) {
            return value.isNumber() ? value.asDouble() : Double.parseDouble(value.asString().trim());
        }
        if (type == PropertyType.BOOLEAN) {
            return value.isBoolean() ? value.asBoolean() : Boolean.parseBoolean(value.asString().trim());
        }
        if (type == PropertyType.DATE) {
            return Instant.parse(value.asString().trim());
        }
        if (type == PropertyType.UUID || type == PropertyType.LIST_ITEM) {
            return value.asString();
        }
        return value.asString();
    }

    private static UUID toUuid(Object value) {
        return switch (value) {
            case UUID uuid -> uuid;
            case String s -> UUID.fromString(s);
            case byte[] bytes when bytes.length == 16 -> {
                long msb = 0;
                long lsb = 0;
                for (int i = 0; i < 8; i++) {
                    msb = (msb << 8) | (bytes[i] & 0xff);
                }
                for (int i = 8; i < 16; i++) {
                    lsb = (lsb << 8) | (bytes[i] & 0xff);
                }
                yield new UUID(msb, lsb);
            }
            default -> throw new IllegalStateException("Unexpected id type from search query: " + value.getClass());
        };
    }
}
