package com.openrecordsmanager.config;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

public class DatabaseConfigContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfigContextInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        var environment = applicationContext.getEnvironment();

        DataSourceProperties readOnlyProps = Binder.get(environment)
                .bind(BuiltinConfigs.DATABASE_READ_ONLY.key(), DataSourceProperties.class)
                .orElse(null);
        DataSourceProperties primaryProps = Binder.get(environment)
                .bind(BuiltinConfigs.DATABASE_PRIMARY.key(), DataSourceProperties.class)
                .orElse(null);

        JdbcTemplate jdbcTemplate = this.connect(readOnlyProps, primaryProps);
        if (jdbcTemplate != null) {
            applicationContext.getEnvironment().getPropertySources().addLast(new DatabaseConfigSource(jdbcTemplate));
        }
    }

    private @Nullable JdbcTemplate connect(@Nullable DataSourceProperties readOnlyProps, @Nullable DataSourceProperties primaryProps) {
        if (readOnlyProps != null && StringUtils.hasText(readOnlyProps.getUrl())) {
            JdbcTemplate readOnly = this.tryConnect(readOnlyProps);
            if (readOnly != null) {
                return readOnly;
            }
        }

        JdbcTemplate primary = this.tryConnect(primaryProps);
        if (primary == null) {
            LOGGER.warn("No database available for config property source; using file and environment defaults only");
        }
        return primary;
    }

    private @Nullable JdbcTemplate tryConnect(@Nullable DataSourceProperties props) {
        if (props == null || !StringUtils.hasText(props.getUrl())) {
            return null;
        }

        try {
            DataSourceBuilder<?> builder = DataSourceBuilder.create()
                    .url(props.getUrl())
                    .username(props.getUsername())
                    .password(props.getPassword());

            if (props.getDriverClassName() != null) {
                builder.driverClassName(props.getDriverClassName());
            }

            DataSource dataSource = builder.build();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return jdbcTemplate;
        } catch (Exception e) {
            LOGGER.debug("Database unavailable at {}: {}", props.getUrl(), e.getMessage());
            return null;
        }
    }
}
