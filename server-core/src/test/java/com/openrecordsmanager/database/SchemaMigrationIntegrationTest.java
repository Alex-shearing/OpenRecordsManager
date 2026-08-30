package com.openrecordsmanager.database;

import com.openrecordsmanager.database.dto.SchemaValidationResponse;
import com.openrecordsmanager.database.schema.SchemaMigrationService;
import com.openrecordsmanager.database.schema.SchemaMigrationState;
import com.openrecordsmanager.database.schema.SchemaValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SchemaMigrationIntegrationTest {

    @Autowired
    private SchemaMigrationService schemaMigrationService;

    @Autowired
    private SchemaMigrationState schemaMigrationState;

    @Autowired
    private SchemaValidationService schemaValidationService;

    @Autowired
    private DataSource writeDataSource;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void emptyDatabaseIsMigratedAtStartupAndReady() throws Exception {
        assertFalse(this.schemaMigrationState.isUpgradeRequired());
        assertEquals(SchemaMigrationState.Status.READY, this.schemaMigrationState.getStatus());
        assertTrue(tableExists("user_details"));
        assertTrue(tableExists("flyway_schema_history"));

        SchemaValidationResponse validationResponse = this.schemaValidationService.validate();
        assertTrue(validationResponse.validated(), validationResponse.message());
        assertNull(validationResponse.message());
    }

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

    private boolean tableExists(String tableName) throws Exception {
        try (Connection connection = this.writeDataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            if (tables.next()) {
                return true;
            }
        }
        // H2 may preserve case depending on mode — also try lowercase
        try (Connection connection = this.writeDataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
            if (tables.next()) {
                return true;
            }
        }
        try (Connection connection = this.writeDataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_NAME) = '" + tableName.toLowerCase() + "'"
             )) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }
}
