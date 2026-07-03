package com.openrecordsmanager.config;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class DatabaseConfigContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        DataSourceProperties dsProps = Binder.get(applicationContext.getEnvironment())
                .bind("spring.datasource", DataSourceProperties.class)
                .orElse(null);

        if (dsProps != null && dsProps.getUrl() != null) {
            DataSource dataSource = DataSourceBuilder.create()
                    .url(dsProps.getUrl())
                    .username(dsProps.getUsername())
                    .password(dsProps.getPassword())
                    .driverClassName(dsProps.getDriverClassName())
                    .build();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            applicationContext.getEnvironment().getPropertySources().addLast(new DatabaseConfigSource(jdbcTemplate));
        }

    }
}
