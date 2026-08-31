package com.openrecordsmanager.database;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.database.schema.SchemaMigrationState;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PrimaryOfflineIntegrationTest {

    private static final Path READ_DB = Path.of("build/test-primary-offline-read.db");

    static {
        try {
            Files.deleteIfExists(READ_DB);
            Files.createDirectories(READ_DB.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Flyway.configure()
                .dataSource("jdbc:sqlite:" + READ_DB + "?busy_timeout=5000", "", "")
                .locations("classpath:db/migration/sqlite")
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void primaryOffline(DynamicPropertyRegistry registry) {
        registry.add("server.database.primary.url",
                () -> "jdbc:sqlite:file:/does/not/exist/orm_primary_offline.db");
        registry.add("server.database.primary.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("server.database.read-only.url",
                () -> "jdbc:sqlite:" + READ_DB + "?open_mode=1");
        registry.add("server.database.read-only.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add(BuiltinConfigs.PLUGINS_SKIP_STARTUP_CHECK.key(), () -> "true");
        registry.add(BuiltinConfigs.DATABASE_PROBE_INTERVAL_MS.key(), () -> "60000");
        registry.add(BuiltinConfigs.AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS.key(), () -> "60000");
    }

    @Autowired
    private DatabaseWritableProbe probe;

    @Autowired
    private SchemaMigrationState schemaMigrationState;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void applicationStartsWithOfflinePrimaryAndServesReadRequests() throws Exception {
        assertFalse(this.probe.isWritable());
        assertFalse(this.schemaMigrationState.isUpgradeRequired());

        this.mockMvc.perform(get("/api/database/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("READY"));

        this.mockMvc.perform(get("/api/auth/providers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
