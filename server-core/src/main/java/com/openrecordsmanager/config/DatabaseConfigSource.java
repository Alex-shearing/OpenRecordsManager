package com.openrecordsmanager.config;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseConfigSource extends EnumerablePropertySource<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfigSource.class);

    private final JdbcTemplate repository;

    public DatabaseConfigSource(JdbcTemplate repository) {
        super("database_config_source");
        this.repository = repository;
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

    @Override
    public String[] getPropertyNames() {
        Set<String> names = new HashSet<>();

        try {
            this.repository.query("SELECT config_key FROM system_configurations", (rs, rowNum) ->
                    names.add(rs.getString("config_key"))
            );
        } catch (Exception e) {
            LOGGER.debug("Failed to list config keys from database: {}", e.getMessage());
        }

        return names.toArray(new String[0]);
    }
}
