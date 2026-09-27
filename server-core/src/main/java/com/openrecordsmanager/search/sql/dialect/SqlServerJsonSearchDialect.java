package com.openrecordsmanager.search.sql.dialect;

import java.util.UUID;

final class SqlServerJsonSearchDialect implements JsonSearchDialect {
    @Override
    public String jsonText(String columnRef) {
        return "JSON_VALUE(" + columnRef + ", '$')";
    }

    @Override
    public String jsonNumeric(String columnRef) {
        return "TRY_CAST(JSON_VALUE(" + columnRef + ", '$') AS FLOAT)";
    }

    @Override
    public String like(String expr, String paramPlaceholder) {
        return "LOWER(" + expr + ") LIKE LOWER(" + paramPlaceholder + ")";
    }

    @Override
    public String quoteIdent(String ident) {
        return "[" + ident + "]";
    }

    @Override
    public String withLimit(int limit) {
        return " OFFSET 0 ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public String convertUuid(UUID value) {
        return value.toString();
    }
}
