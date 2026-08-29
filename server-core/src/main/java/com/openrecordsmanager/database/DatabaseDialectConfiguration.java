package com.openrecordsmanager.database;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Resolves the Hibernate dialect from the JDBC URL without contacting the database.
 * Required so the app can start when the primary is offline and only a read replica is reachable.
 *
 * <p>Also overrides Spring's default {@code DELAYED_ACQUISITION_AND_HOLD} connection mode.
 * HOLD keeps the first JDBC connection for the whole OSIV session, so a read-only request
 * that later writes (e.g. audit) would reuse the replica connection. Release-after-transaction
 * lets {@link TransactionRoutingDataSource} pick write vs read per transaction.
 */
@Configuration
public class DatabaseDialectConfiguration {

    @Bean
    public HibernatePropertiesCustomizer ormHibernateProperties(
            DataSourceProperties primaryDataSourceProperties,
            DataSourceProperties readOnlyDataSourceProperties
    ) {
        String jdbcUrl = jdbcUrl(primaryDataSourceProperties, readOnlyDataSourceProperties);
        return properties -> {
            properties.put("hibernate.boot.allow_jdbc_metadata_access", false);
            properties.put("hibernate.dialect", dialectClassName(jdbcUrl));
            properties.put(
                    "hibernate.connection.handling_mode",
                    "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION"
            );
        };
    }

    private static String jdbcUrl(
            DataSourceProperties primaryProperties,
            DataSourceProperties readOnlyProperties
    ) {
        if (StringUtils.hasText(primaryProperties.getUrl())) {
            return primaryProperties.getUrl();
        }
        if (StringUtils.hasText(readOnlyProperties.getUrl())) {
            return readOnlyProperties.getUrl();
        }
        throw new IllegalStateException("No JDBC URL configured for Hibernate dialect resolution");
    }

    private static String dialectClassName(String jdbcUrl) {
        String url = jdbcUrl.toLowerCase();
        if (url.contains(":h2:")) {
            return "org.hibernate.dialect.H2Dialect";
        }
        if (url.contains("sqlite")) {
            return "org.hibernate.community.dialect.SQLiteDialect";
        }
        if (url.contains("sqlserver")) {
            return "org.hibernate.dialect.SQLServerDialect";
        }
        throw new IllegalStateException("Unsupported database URL for Hibernate dialect: " + jdbcUrl);
    }
}
