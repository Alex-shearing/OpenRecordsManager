package com.openrecordsmanager.config;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Exposes {@code system_configurations} rows to Spring's {@link org.springframework.core.env.Environment}.
 * <p>
 * Intentionally <strong>not</strong> an {@link org.springframework.core.env.EnumerablePropertySource}:
 * Spring Boot's configuration-property cache calls {@code getPropertyNames()} on enumerable sources,
 * which would {@code SELECT} the whole table on a connection outside the app's JPA transaction and
 * deadlock with in-flight config writes (especially on SQL Server).
 * <p>
 * Runtime reads through {@link ConfigService} skip this source and use the transactional repository
 * instead; this source remains for Binder/{@code @ConfigurationProperties} and other Environment users,
 * with a short query timeout so lock waits fail closed rather than hang.
 */
public class DatabaseConfigSource extends PropertySource<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfigSource.class);

    /** Fail fast under lock contention instead of deadlocking writers. */
    private static final int QUERY_TIMEOUT_SECONDS = 2;

    private final JdbcTemplate repository;

    public DatabaseConfigSource(JdbcTemplate repository) {
        super("database_config_source");
        this.repository = repository;
        this.repository.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
    }

    @Override
    public @Nullable Object getProperty(String name) {
        // Do not attempt to load server. configs from the database
        if (name.startsWith("server.")) {
            return null;
        }

        try {
            List<String> vals = this.repository.query(
                    "SELECT config_value FROM system_configurations WHERE config_key = ?",
                    (rs, _) -> rs.getString("config_value"),
                    name
            );
            if (vals.size() != 1) {
                return null;
            }
            return vals.getFirst();
        } catch (Exception e) {
            LOGGER.debug("Failed to load config key '{}' from database: {}", name, e.getMessage());
            return null;
        }
    }
}
