package com.openrecordsmanager.search.sql.dialect;

import com.openrecordsmanager.database.DatabaseVendor;

import java.util.UUID;

/**
 * Vendor-specific SQL helpers for comparing JSON-stored property values and case-insensitive LIKE.
 */
public interface JsonSearchDialect {

    /**
     * Expression that yields a text scalar from a JSON column (unwraps JSON string quotes).
     */
    String jsonText(String columnRef);

    /**
     * Expression that yields a numeric scalar from a JSON column.
     */
    String jsonNumeric(String columnRef);

    /**
     * Case-insensitive LIKE of {@code expr} against a bound parameter placeholder.
     */
    String like(String expr, String paramPlaceholder);

    /**
     * Quote a SQL identifier for this dialect.
     */
    String quoteIdent(String ident);

    /**
     * Vendor-specific LIMIT / FETCH clause (including leading space) for a SELECT that already has ORDER BY.
     */
    String withLimit(int limit);

    /**
     * Convert a UUID into the JDBC bind form for this dialect (native UUID, string, or 16-byte blob).
     */
    Object convertUuid(UUID value);

    static JsonSearchDialect of(DatabaseVendor vendor) {
        return switch (vendor) {
            case POSTGRESQL -> new PostgresqlJsonSearchDialect();
            case MARIADB -> new MariaDbJsonSearchDialect();
            case SQLITE -> new SqliteJsonSearchDialect();
            case SQLSERVER -> new SqlServerJsonSearchDialect();
        };
    }
}
