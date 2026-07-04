package com.openrecordsmanager.config;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseConfigContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        DataSourceProperties dsProps = Binder.get(applicationContext.getEnvironment())
                .bind(BuiltinConfigs.DATABASE_URL.key(), DataSourceProperties.class)
                .orElse(null);

        if (dsProps != null && dsProps.getUrl() != null) {
            DataSourceBuilder<?> dataSource = DataSourceBuilder.create()
                    .url(dsProps.getUrl())
                    .username(dsProps.getUsername())
                    .password(dsProps.getPassword());

            if (dsProps.getDriverClassName() != null) {
                dataSource.driverClassName(dsProps.getDriverClassName());
            }

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource.build());

            applicationContext.getEnvironment().getPropertySources().addLast(new DatabaseConfigSource(jdbcTemplate));
        }
    }
}
