package com.openrecordsmanager.database.schema;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * In-memory schema migration readiness shared by startup strategy, API, and gate filter.
 */
public final class SchemaMigrationState {

    public enum Status {
        READY,
        UPGRADE_REQUIRED
    }

    private Status status = Status.READY;
    private @Nullable String currentVersion;
    private List<String> pendingMigrations = List.of();
    private @Nullable String message;
    private boolean initialSeedPending;

    public Status getStatus() {
        return this.status;
    }

    public boolean isUpgradeRequired() {
        return this.status == Status.UPGRADE_REQUIRED;
    }

    public @Nullable String getCurrentVersion() {
        return this.currentVersion;
    }

    public List<String> getPendingMigrations() {
        return this.pendingMigrations;
    }

    public @Nullable String getMessage() {
        return this.message;
    }

    public void markReady(@Nullable String currentVersion) {
        this.status = Status.READY;
        this.currentVersion = currentVersion;
        this.pendingMigrations = List.of();
        this.message = null;
    }

    public void markUpgradeRequired(@Nullable String currentVersion, List<String> pending, String message) {
        this.status = Status.UPGRADE_REQUIRED;
        this.currentVersion = currentVersion;
        this.pendingMigrations = List.copyOf(pending);
        this.message = message;
    }

    /**
     * Mark that this process created a brand-new schema and should seed bootstrap data
     * once JPA repositories are available (after Flyway / EntityManagerFactory startup).
     */
    public void markInitialSeedPending() {
        this.initialSeedPending = true;
    }

    /**
     * @return true once if an initial seed was requested; subsequent calls return false
     */
    public boolean consumeInitialSeedPending() {
        if (!this.initialSeedPending) {
            return false;
        }
        this.initialSeedPending = false;
        return true;
    }
}
