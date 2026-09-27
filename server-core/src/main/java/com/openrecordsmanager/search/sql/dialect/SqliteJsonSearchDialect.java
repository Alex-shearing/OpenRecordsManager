package com.openrecordsmanager.search.sql.dialect;

import java.nio.ByteBuffer;
import java.util.UUID;

final class SqliteJsonSearchDialect implements JsonSearchDialect {
    @Override
    public String jsonText(String columnRef) {
        return "json_extract(" + columnRef + ", '$')";
    }

    @Override
    public String jsonNumeric(String columnRef) {
        return "CAST(json_extract(" + columnRef + ", '$') AS REAL)";
    }

    @Override
    public String like(String expr, String paramPlaceholder) {
        return "LOWER(" + expr + ") LIKE LOWER(" + paramPlaceholder + ")";
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
    public byte[] convertUuid(UUID value) {
        return ByteBuffer.allocate(16)
                .putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits())
                .array();
    }
}
