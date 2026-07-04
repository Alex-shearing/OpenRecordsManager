package com.openrecordsmanager.database;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
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
        return primaryDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
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

        return readProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    public DataSource routingDataSource() {
        TransactionRoutingDataSource routingDataSource = new TransactionRoutingDataSource();
        routingDataSource.setTargetDataSources(Map.of(
                DataSourceType.READ_WRITE, writeDataSource(),
                DataSourceType.READ_ONLY, readDataSource()
        ));

        // Default fallback to read-write database
        routingDataSource.setDefaultTargetDataSource(writeDataSource());

        return routingDataSource;
    }

    @Primary
    @Bean
    public DataSource dataSource() {
        return new LazyConnectionDataSourceProxy(routingDataSource());
    }
}
