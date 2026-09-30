package com.openrecordsmanager.database;

import com.openrecordsmanager.search.sql.dialect.JsonSearchDialect;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sets an explicit Hibernate dialect from the JDBC URL so the SessionFactory can start
 * without requiring a live primary connection.
 * <p>
 * When the primary is offline, metadata access fails and Hibernate falls back to this
 * explicit dialect so startup still succeeds.
 *
 * <p>Also overrides Spring's default {@code DELAYED_ACQUISITION_AND_HOLD} connection mode.
 * HOLD keeps the first JDBC connection for the whole OSIV session, so a read-only request
 * that later writes (e.g. audit) would reuse the replica connection. Release-after-transaction
 * lets {@link TransactionRoutingDataSource} pick write vs read per transaction.
 */
@Configuration
public class DatabaseDialectConfiguration {

    @Bean
    public DatabaseVendor databaseVendor(
            DataSourceProperties primaryDataSourceProperties,
            DataSourceProperties readOnlyDataSourceProperties
    ) {
        return DatabaseVendor.fromDataSourceProperties(
                primaryDataSourceProperties,
                readOnlyDataSourceProperties
        );
    }

    @Bean
    public JsonSearchDialect jsonSearchDialect(DatabaseVendor vendor) {
        return JsonSearchDialect.of(vendor);
    }

    @Bean
    public HibernatePropertiesCustomizer ormHibernateProperties(DatabaseVendor vendor) {
        return properties -> {
            properties.put("hibernate.dialect", vendor.hibernateDialectClassName());
            properties.put(
                    "hibernate.connection.handling_mode",
                    "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION"
            );
            // Spring Boot 4 / Hibernate 7.2 auto-picks Jackson 2 when both Jackson versions exist;
            // property values typed as tools.jackson.databind.JsonNode need Jackson 3.
            properties.put("hibernate.type.json_format_mapper", new ToolsJacksonJsonFormatMapper());
        };
    }
}
