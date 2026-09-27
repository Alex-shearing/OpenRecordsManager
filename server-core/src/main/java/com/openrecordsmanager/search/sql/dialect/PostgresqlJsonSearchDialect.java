package com.openrecordsmanager.search.sql.dialect;

import java.util.UUID;

final class PostgresqlJsonSearchDialect implements JsonSearchDialect {
    @Override
    public String jsonText(String columnRef) {
        return "(" + columnRef + " #>> '{}')";
    }

    @Override
    public String jsonNumeric(String columnRef) {
        return "((" + columnRef + " #>> '{}')::numeric)";
    }

    @Override
    public String like(String expr, String paramPlaceholder) {
        return expr + " ILIKE " + paramPlaceholder;
    }

    @Override
    public String quoteIdent(String ident) {
        return "\"" + ident + "\"";
    }

    @Override
    public String withLimit(int limit) {
        return " LIMIT " + limit;
    }

    @Override
    public UUID convertUuid(UUID value) {
        return value;
    }
}
