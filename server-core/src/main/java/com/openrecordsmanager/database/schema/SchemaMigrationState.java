package com.openrecordsmanager.database.schema;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * In-memory schema migration readiness shared by startup strategy, API, and gate filter.
 */
public final class SchemaMigrationState {

    public enum Status {
        READY,
        UPGRADE_REQUIRED,
        SETUP_REQUIRED
    }

    private Status status = Status.READY;
    private @Nullable String currentVersion;
    private List<String> pendingMigrations = List.of();

    public Status getStatus() {
        return this.status;
    }

    public boolean isUpgradeRequired() {
        return this.status == Status.UPGRADE_REQUIRED;
    }

    public boolean isSetupRequired() {
        return this.status == Status.SETUP_REQUIRED;
    }

    public @Nullable String getCurrentVersion() {
        return this.currentVersion;
    }

    public List<String> getPendingMigrations() {
        return this.pendingMigrations;
    }

    public void markReady(@Nullable String currentVersion) {
        this.status = Status.READY;
        this.currentVersion = currentVersion;
        this.pendingMigrations = List.of();
    }

    public void markSetupRequired(@Nullable String currentVersion) {
        this.status = Status.SETUP_REQUIRED;
        this.currentVersion = currentVersion;
        this.pendingMigrations = List.of();
    }

    public void markUpgradeRequired(@Nullable String currentVersion, List<String> pending) {
        this.status = Status.UPGRADE_REQUIRED;
        this.currentVersion = currentVersion;
        this.pendingMigrations = List.copyOf(pending);
    }
}
