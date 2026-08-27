package com.openrecordsmanager.database.schema;

import com.openrecordsmanager.database.dto.SetupStatusResponse;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class SchemaMigrationService {

    private final Flyway flyway;
    private final SchemaMigrationState state;

    public SchemaMigrationService(Flyway flyway, SchemaMigrationState state) {
        this.flyway = flyway;
        this.state = state;
    }

    public SetupStatusResponse upgrade() {
        MigrateResult result = this.flyway.migrate();
        this.evaluate();
        if (this.state.isUpgradeRequired()) {
            throw new IllegalStateException("Schema upgrade completed but pending migrations remain");
        }
        return this.toStatusResponse("Applied " + result.migrationsExecuted + " migration(s)");
    }

    public void evaluate() {
        MigrationInfo[] pending = this.flyway.info().pending();
        MigrationInfo[] applied = this.flyway.info().applied();

        if (applied.length == 0) {
            // This is a brand-new database, apply the schema to create the database
            this.upgrade();
        } else if (pending.length > 0) {
            this.state.markUpgradeRequired(
                    currentVersion(applied),
                    pendingVersions(pending),
                    "Database schema is behind the application and must be upgraded"
            );
        } else {
            this.state.markReady(currentVersion(applied));
        }
    }

    public SetupStatusResponse toStatusResponse(@Nullable String messageOverride) {
        return new SetupStatusResponse(
                this.state.getStatus(),
                this.state.getCurrentVersion(),
                this.state.getPendingMigrations(),
                messageOverride == null ? this.state.getMessage() : messageOverride
        );
    }

    private static @Nullable String currentVersion(MigrationInfo[] applied) {
        if (applied.length == 0) {
            return null;
        }
        MigrationInfo last = applied[applied.length - 1];
        return last.getVersion() != null ? last.getVersion().toString() : last.getDescription();
    }

    private static List<String> pendingVersions(MigrationInfo[] pending) {
        return Arrays.stream(pending)
                .map(info -> {
                    String version = info.getVersion() != null ? info.getVersion().toString() : "";
                    return version + " - " + info.getDescription();
                })
                .toList();
    }
}
