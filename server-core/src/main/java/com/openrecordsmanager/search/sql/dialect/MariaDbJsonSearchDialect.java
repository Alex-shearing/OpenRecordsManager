package com.openrecordsmanager.search.sql.dialect;

import java.util.UUID;

final class MariaDbJsonSearchDialect implements JsonSearchDialect {
    @Override
    public String jsonText(String columnRef) {
        return "JSON_UNQUOTE(" + columnRef + ")";
    }

    @Override
    public String jsonNumeric(String columnRef) {
        return "CAST(JSON_UNQUOTE(" + columnRef + ") AS DECIMAL(38, 10))";
    }

    @Override
    public String like(String expr, String paramPlaceholder) {
        return "LOWER(" + expr + ") LIKE LOWER(" + paramPlaceholder + ")";
    }

    @Override
    public String quoteIdent(String ident) {
        return "`" + ident + "`";
    }

    @Override
    public String withLimit(int limit) {
        return " LIMIT " + limit;
    }

    @Override
    public String convertUuid(UUID value) {
        return value.toString();
    }
}
