package com.openrecordsmanager.database;

import com.openrecordsmanager.database.schema.SchemaMigrationState;
import com.openrecordsmanager.database.schema.SchemaValidationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;

@Tag("database-migration")
@SpringBootTest
class SqliteSchemaMigrationIntegrationTest {

    private static final String JDBC_URL = "jdbc:sqlite::memory:";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("server.database.primary.url", () -> JDBC_URL);
        registry.add("server.database.primary.driver-class-name", () -> "org.sqlite.JDBC");
        VendorSchemaMigrationTestSupport.registerCommonProperties(registry);
    }

    @Autowired
    private SchemaMigrationState schemaMigrationState;

    @Autowired
    private SchemaValidationService schemaValidationService;

    @Autowired
    @Qualifier("writeDataSource")
    private DataSource writeDataSource;

    @Test
    void migratesAndValidatesSchema() throws Exception {
        VendorSchemaMigrationTestSupport.assertMigratedAndValid(
                this.schemaMigrationState,
                this.schemaValidationService,
                this.writeDataSource,
                JDBC_URL
        );
    }
}
