package com.openrecordsmanager.database;

import com.openrecordsmanager.database.dto.SchemaValidationResponse;
import com.openrecordsmanager.database.schema.SchemaMigrationState;
import com.openrecordsmanager.database.schema.SchemaValidationService;
import org.springframework.test.context.DynamicPropertyRegistry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VendorSchemaMigrationTestSupport {

    private static final Set<String> CORE_TABLES = Set.of(
            "system_configurations",
            "auth_provider",
            "list_type",
            "object_property",
            "list_element",
            "list_element_alias",
            "record_type",
            "record_type_property",
            "user_details",
            "user_property_value",
            "auth_token",
            "file_store",
            "file_store_middleware",
            "file_store_middleware_usage",
            "file_store_entry",
            "plugin",
            "record",
            "record_property_value",
            "record_revision",
            "audit_event",
            "audit_policy",
            "flyway_schema_history"
    );

    private VendorSchemaMigrationTestSupport() {
    }

    static void registerCommonProperties(DynamicPropertyRegistry registry) {
        registry.add("server.database.read-only.url", () -> "");
        registry.add("app.plugins.skip_sync", () -> "true");
        registry.add("server.plugins.directory", () -> "./plugins");
    }

    static void assertMigratedAndValid(
            SchemaMigrationState schemaMigrationState,
            SchemaValidationService schemaValidationService,
            DataSource dataSource,
            String jdbcUrl
    ) throws SQLException {
        assertEquals(SchemaMigrationState.Status.READY, schemaMigrationState.getStatus());
        assertCoreTablesPresent(dataSource);

        if (jdbcUrl.toLowerCase().contains("sqlite")) {
            // Hibernate schema validation cannot reliably read SQLite metadata when JDBC metadata access is disabled.
            return;
        }

        SchemaValidationResponse validationResponse = schemaValidationService.validate();
        assertTrue(validationResponse.validated(), validationResponse.message());
        assertNull(validationResponse.message());
    }

    private static void assertCoreTablesPresent(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            for (String tableName : CORE_TABLES) {
                assertTrue(
                        tableExists(metadata, tableName),
                        () -> "Expected table '" + tableName + "' to exist after Flyway migration"
                );
            }
        }
    }

    private static boolean tableExists(DatabaseMetaData metadata, String tableName) throws SQLException {
        if (hasTable(metadata, tableName)) {
            return true;
        }
        if (hasTable(metadata, tableName.toUpperCase())) {
            return true;
        }
        return hasTable(metadata, tableName.toLowerCase());
    }

    private static boolean hasTable(DatabaseMetaData metadata, String tableName) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }
}
