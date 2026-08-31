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
import org.testcontainers.mssqlserver.MSSQLServerContainer;

import javax.sql.DataSource;

@Tag("database-migration")
@Testcontainers
@SpringBootTest
class SqlServerSchemaMigrationIntegrationTest {

    @Container
    static MSSQLServerContainer sqlServer = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("server.database.primary.url", sqlServer::getJdbcUrl);
        registry.add("server.database.primary.username", sqlServer::getUsername);
        registry.add("server.database.primary.password", sqlServer::getPassword);
        registry.add("server.database.primary.driver-class-name", () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
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
                sqlServer.getJdbcUrl()
        );
    }
}
