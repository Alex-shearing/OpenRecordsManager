package com.openrecordsmanager.database.schema;

import com.openrecordsmanager.database.dto.DatabaseStatusResponse;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

@Service
public class SchemaMigrationService {

    private final Flyway flyway;
    private final DataSource writeDataSource;
    private final SchemaMigrationState state;
    private final ApplicationEventPublisher events;

    public SchemaMigrationService(
            Flyway flyway,
            @Qualifier("writeDataSource") DataSource writeDataSource,
            SchemaMigrationState state,
            ApplicationEventPublisher events
    ) {
        this.flyway = flyway;
        this.writeDataSource = writeDataSource;
        this.state = state;
        this.events = events;
    }

    public int upgrade() {
        MigrateResult result = this.writableFlyway().migrate();
        this.evaluate();
        if (this.state.isUpgradeRequired()) {
            throw new IllegalStateException("Schema upgrade completed but pending migrations remain");
        }
        this.events.publishEvent(new SchemaMigrationReadyEvent());
        return result.migrationsExecuted;
    }

    public void evaluate() {
        MigrationInfo[] pending = this.flyway.info().pending();
        MigrationInfo[] applied = this.flyway.info().applied();

        if (applied.length == 0) {
            // Brand-new database: apply schema now so the app can boot (JPA/services need tables).
            this.writableFlyway().migrate();
            this.evaluate();
        } else if (pending.length > 0) {
            this.state.markUpgradeRequired(currentVersion(applied), pendingVersions(pending));
        } else {
            this.state.markReady(currentVersion(applied));
        }
    }

    private Flyway writableFlyway() {
        return Flyway.configure()
                .configuration(this.flyway.getConfiguration())
                .dataSource(this.writeDataSource)
                .load();
    }

    public DatabaseStatusResponse toStatusResponse(int migrationsApplied, boolean writable) {
        return new DatabaseStatusResponse(
                this.state.getStatus(),
                this.state.getCurrentVersion(),
                this.state.getPendingMigrations(),
                migrationsApplied,
                writable
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
