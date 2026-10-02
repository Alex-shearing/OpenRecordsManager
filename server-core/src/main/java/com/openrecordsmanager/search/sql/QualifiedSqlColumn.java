package com.openrecordsmanager.search.sql;

import com.openrecordsmanager.search.sql.dialect.JsonSearchDialect;

/**
 * A physical table and column used in search SQL.
 */
public record QualifiedSqlColumn(String tableName, String columnName) {

    public String tableName(JsonSearchDialect dialect) {
        return dialect.quoteIdent(this.tableName);
    }

    public String column(JsonSearchDialect dialect) {
        return dialect.quoteIdent(this.tableName) + "." + dialect.quoteIdent(this.columnName);
    }
}
