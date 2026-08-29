package com.openrecordsmanager.audit;

import com.openrecordsmanager.database.schema.SchemaMigrationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DatabaseWritableProbe {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseWritableProbe.class);

    private final DataSource writeDataSource;
    private final SchemaMigrationState migrationState;
    private final AtomicBoolean writable = new AtomicBoolean(true);
    private final AtomicReference<Instant> lastChecked = new AtomicReference<>(Instant.EPOCH);
    private final AtomicReference<Instant> lastSuccessfulWrite = new AtomicReference<>(Instant.EPOCH);

    public DatabaseWritableProbe(
            @Qualifier("writeDataSource") DataSource writeDataSource,
            SchemaMigrationState migrationState
    ) {
        this.writeDataSource = writeDataSource;
        this.migrationState = migrationState;
    }

    public boolean isWritable() {
        return this.writable.get();
    }

    public Instant getLastChecked() {
        return this.lastChecked.get();
    }

    public Instant getLastSuccessfulWrite() {
        return this.lastSuccessfulWrite.get();
    }

    public void markWriteSucceeded() {
        this.writable.set(true);
        this.lastSuccessfulWrite.set(Instant.now());
    }

    public void markWriteFailed() {
        this.writable.set(false);
    }

    @Scheduled(fixedDelayString = "${audit.probe.interval-ms:30000}")
    public void probe() {
        if (this.migrationState.isUpgradeRequired()) {
            this.writable.set(false);
            return;
        }
        this.lastChecked.set(Instant.now());
        try (Connection connection = this.writeDataSource.getConnection()) {
            if (connection.isReadOnly()) {
                this.writable.set(false);
                return;
            }
            connection.createStatement().execute("SELECT 1");
            this.writable.set(true);
        } catch (Exception e) {
            LOGGER.debug("Primary database is not writable: {}", e.getMessage());
            this.writable.set(false);
        }
    }
}
