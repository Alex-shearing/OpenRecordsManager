package com.openrecordsmanager.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.openrecordsmanager.audit.DatabaseWritableProbe;
import com.openrecordsmanager.database.schema.SchemaMigrationState;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PrimaryOfflineIntegrationTest {

    private static final String READ_DB = "orm_primary_offline_read";

    static {
        Flyway.configure()
                .dataSource("jdbc:h2:mem:" + READ_DB + ";DB_CLOSE_DELAY=-1;MODE=MySQL", "sa", "")
                .locations("classpath:db/migration/h2")
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void primaryOffline(DynamicPropertyRegistry registry) {
        registry.add("server.database.primary.url",
                () -> "jdbc:h2:tcp://127.0.0.1:59999/offline;connectTimeout=1000");
        registry.add("server.database.primary.username", () -> "sa");
        registry.add("server.database.primary.password", () -> "");
        registry.add("server.database.primary.driver-class-name", () -> "org.h2.Driver");
        registry.add("server.database.read-only.url",
                () -> "jdbc:h2:mem:" + READ_DB + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
        registry.add("server.database.read-only.username", () -> "sa");
        registry.add("server.database.read-only.password", () -> "");
        registry.add("server.database.read-only.driver-class-name", () -> "org.h2.Driver");
        registry.add("server.plugins.skip_startup_check", () -> "true");
        registry.add("audit.probe.interval-ms", () -> "60000");
        registry.add("audit.drain.fixed-delay-ms", () -> "60000");
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
