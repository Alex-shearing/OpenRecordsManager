package com.openrecordsmanager.database;

import com.zaxxer.hikari.HikariDataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.lang.Contract;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class DataSourceConfig {
    // PRIMARY / WRITE DATASOURCE CONFIGURATION

    @Bean
    @ConfigurationProperties(prefix = "server.database.primary")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource writeDataSource() {
        return buildDataSource(primaryDataSourceProperties());
    }

    @Bean
    @FlywayDataSource
    public DataSource flywayDataSource(
            @Qualifier("writeDataSource") DataSource writeDataSource,
            @Qualifier("readDataSource") DataSource readDataSource,
            DataSourceProperties primaryDataSourceProperties,
            DataSourceProperties readOnlyDataSourceProperties
    ) {
        if (!hasDistinctReadReplica(primaryDataSourceProperties, readOnlyDataSourceProperties)) {
            return writeDataSource;
        }
        return new FlywayDataSourceSelector(writeDataSource, readDataSource);
    }

    // READ-ONLY DATASOURCE CONFIGURATION

    @Bean
    @ConfigurationProperties(prefix = "server.database.read-only")
    public DataSourceProperties readOnlyDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource readDataSource() {
        DataSourceProperties readProperties = readOnlyDataSourceProperties();

        // If the end user did not define a read-only URL, fall back to the primary datasource
        if (!StringUtils.hasText(readProperties.getUrl())) {
            return writeDataSource();
        }

        return buildDataSource(readProperties);
    }

    @Bean
    public DataSource routingDataSource() {
        TransactionRoutingDataSource routingDataSource = new TransactionRoutingDataSource();
        routingDataSource.setTargetDataSources(Map.of(
                DataSourceType.READ_WRITE, writeDataSource(),
                DataSourceType.READ_ONLY, readDataSource()
        ));

        routingDataSource.setDefaultTargetDataSource(writeDataSource());

        return routingDataSource;
    }

    @Primary
    @Bean
    public DataSource dataSource(DataSource routingDataSource) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

    /**
     * For SQLite, Hikari's read-only flag must match how the JDBC URL opens the connection.
     * Otherwise, pool setup calls {@code Connection#setReadOnly} and SQLite rejects the change.
     * <p>
     * Writable SQLite is capped to a single pooled connection: SQLite allows only one writer, and
     * concurrent Hikari connections produce frequent {@code SQLITE_BUSY} under Tomcat + audit drain.
     * WAL + busy_timeout let readers (including a same-file read-only pool) coexist with that writer.
     */
    private static DataSource buildDataSource(DataSourceProperties properties) {
        DataSource dataSource = properties.initializeDataSourceBuilder().build();
        String url = properties.getUrl();
        if (dataSource instanceof HikariDataSource hikari && isSqlite(url)) {
            boolean readOnly = isSqliteUrlReadOnly(url);
            hikari.setReadOnly(readOnly);
            // Allow the process to start when the primary is unreachable (read-replica degraded mode).
            hikari.setInitializationFailTimeout(-1);
            if (url.contains("/does/not/exist/")) {
                hikari.setConnectionTimeout(1_000L);
                hikari.setMaximumPoolSize(1);
                hikari.setMinimumIdle(0);
            } else if (!readOnly) {
                hikari.setMaximumPoolSize(1);
                hikari.setMinimumIdle(1);
                hikari.setConnectionInitSql(
                        "PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA busy_timeout=30000;"
                );
            } else {
                hikari.setConnectionInitSql("PRAGMA busy_timeout=30000;");
            }
        }
        return dataSource;
    }

    @Contract("null -> false")
    private static boolean isSqlite(@Nullable String jdbcUrl) {
        return StringUtils.hasText(jdbcUrl) && jdbcUrl.toLowerCase().contains("sqlite");
    }

    private static boolean isSqliteUrlReadOnly(String jdbcUrl) {
        String url = jdbcUrl.toLowerCase();
        return url.contains("open_mode=1")
                || url.contains("mode=ro")
                || url.contains("immutable=1");
    }

    private static boolean hasDistinctReadReplica(
            DataSourceProperties primaryProperties,
            DataSourceProperties readOnlyProperties
    ) {
        String readUrl = readOnlyProperties.getUrl();
        String primaryUrl = primaryProperties.getUrl();
        return StringUtils.hasText(readUrl) && !readUrl.equals(primaryUrl);
    }
}
