package com.openrecordsmanager.database.schema;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchemaMigrationConfig {

    @Bean
    public SchemaMigrationState schemaMigrationState() {
        return new SchemaMigrationState();
    }

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(SchemaMigrationService schemaMigrationService) {
        return flyway -> schemaMigrationService.evaluate();
    }
}
