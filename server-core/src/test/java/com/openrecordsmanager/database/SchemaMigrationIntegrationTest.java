package com.openrecordsmanager.database;

import com.openrecordsmanager.database.schema.SchemaMigrationService;
import com.openrecordsmanager.database.schema.SchemaMigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SchemaMigrationIntegrationTest {

    @Autowired
    private SchemaMigrationService schemaMigrationService;

    @Autowired
    private SchemaMigrationState schemaMigrationState;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pendingMigrationRequiresUpgradeAndBlocksBusinessApis() throws Exception {
        // Simulate an older schema: mark state as upgrade required without applying a real pending file
        this.schemaMigrationState.markUpgradeRequired("1", List.of("2 - pending"), "upgrade required");

        this.mockMvc.perform(get("/api/database/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("UPGRADE_REQUIRED"));

        this.mockMvc.perform(get("/api/auth/providers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(SchemaUpgradeGateFilter.UPGRADE_REQUIRED_HEADER, "true"));

        // Restore ready state for other tests in this context
        this.schemaMigrationService.evaluate();
        assertFalse(this.schemaMigrationState.isUpgradeRequired());
    }
}
