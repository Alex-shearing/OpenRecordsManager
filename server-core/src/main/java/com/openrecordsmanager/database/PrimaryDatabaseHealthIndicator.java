package com.openrecordsmanager.database;

import com.openrecordsmanager.database.schema.SchemaMigrationState;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * Reports primary database readiness for ops probes ({@code /api/health}).
 * DOWN when the primary is not writable or a schema upgrade is required.
 */
@Component("primaryDatabase")
public class PrimaryDatabaseHealthIndicator extends AbstractHealthIndicator {

    private final DatabaseWritableProbe probe;
    private final SchemaMigrationState migrationState;

    public PrimaryDatabaseHealthIndicator(DatabaseWritableProbe probe, SchemaMigrationState migrationState) {
        super("Primary database health check failed");
        this.probe = probe;
        this.migrationState = migrationState;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if (this.migrationState.isUpgradeRequired()) {
            builder.down();
            return;
        }
        if (!this.probe.isWritable()) {
            builder.down();
            return;
        }
        builder.up();
    }
}
