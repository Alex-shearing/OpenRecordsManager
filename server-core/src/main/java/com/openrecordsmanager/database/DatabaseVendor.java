package com.openrecordsmanager.database;

import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.util.StringUtils;

/**
 * Database vendor detected from the JDBC URL (no metadata contact).
 */
public enum DatabaseVendor {
    POSTGRESQL("org.hibernate.dialect.PostgreSQLDialect"),
    MARIADB("org.hibernate.dialect.MariaDBDialect"),
    SQLITE("org.hibernate.community.dialect.SQLiteDialect"),
    SQLSERVER("org.hibernate.dialect.SQLServerDialect");

    private final String hibernateDialectClassName;

    DatabaseVendor(String hibernateDialectClassName) {
        this.hibernateDialectClassName = hibernateDialectClassName;
    }

    public String hibernateDialectClassName() {
        return this.hibernateDialectClassName;
    }

    private static String getJdbcUrlFromSource(
            DataSourceProperties primaryProperties,
            DataSourceProperties readOnlyProperties
    ) {
        if (StringUtils.hasText(primaryProperties.getUrl())) {
            return primaryProperties.getUrl();
        }
        if (StringUtils.hasText(readOnlyProperties.getUrl())) {
            return readOnlyProperties.getUrl();
        }
        throw new IllegalStateException("No JDBC URL configured for dialect resolution");
    }

    public static DatabaseVendor fromJdbcUrl(String jdbcUrl) {
        String url = jdbcUrl.toLowerCase();
        if (url.contains("sqlite")) {
            return SQLITE;
        }
        if (url.contains("sqlserver")) {
            return SQLSERVER;
        }
        if (url.contains("postgresql") || url.contains("postgres")) {
            return POSTGRESQL;
        }
        if (url.contains("mariadb") || url.contains("mysql")) {
            return MARIADB;
        }
        throw new IllegalStateException("Unsupported database URL for dialect resolution: " + jdbcUrl);
    }

    public static DatabaseVendor fromDataSourceProperties(
            DataSourceProperties primaryProperties,
            DataSourceProperties readOnlyProperties
    ) {
        return fromJdbcUrl(getJdbcUrlFromSource(primaryProperties, readOnlyProperties));
    }
}
