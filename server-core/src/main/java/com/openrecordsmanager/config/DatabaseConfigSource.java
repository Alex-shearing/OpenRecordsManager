package com.openrecordsmanager.config;

import org.jspecify.annotations.Nullable;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseConfigSource extends EnumerablePropertySource<Object> {
    private final JdbcTemplate repository;

    public DatabaseConfigSource(JdbcTemplate repository) {
        super("database_config_source");
        this.repository = repository;
        this.repository.execute("CREATE TABLE IF NOT EXISTS system_configurations (config_key VARCHAR(255) PRIMARY KEY, config_value VARCHAR(1000) NOT NULL);");
    }

    @Override
    public @Nullable Object getProperty(String name) {
        // Do not attempt to load server. configs from the database
        if (name.startsWith("server.")) {
            return null;
        }

        List<String> vals = this.repository.query(
                "SELECT config_value FROM system_configurations WHERE config_key = ?",
                (rs, _) -> rs.getString("config_value"),
                name
        );
        if (vals.size() != 1) {
            return null;
        }

        return vals.getFirst();
    }

    @Override
    public String[] getPropertyNames() {
        Set<String> names = new HashSet<>();

        this.repository.query("SELECT config_key FROM system_configurations", (rs, rowNum) ->
                names.add(rs.getString("config_key"))
        );

        return names.toArray(new String[0]);
    }
}
