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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

@Tag("database-migration")
@Testcontainers
@SpringBootTest
class PostgreSqlSchemaMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("server.database.primary.url", postgres::getJdbcUrl);
        registry.add("server.database.primary.username", postgres::getUsername);
        registry.add("server.database.primary.password", postgres::getPassword);
        registry.add("server.database.primary.driver-class-name", () -> "org.postgresql.Driver");
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
                postgres.getJdbcUrl()
        );
    }
}
