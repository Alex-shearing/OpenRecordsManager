package com.openrecordsmanager.audit.spool;

import com.openrecordsmanager.audit.AuditEventPayload;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DatabaseWritableProbe;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AuditSpoolDrainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditSpoolDrainer.class);

    private final AuditSpoolWriter spoolWriter;
    private final AuditService auditService;
    private final DatabaseWritableProbe probe;
    private final AtomicReference<Instant> lastDrainAttempt = new AtomicReference<>(Instant.EPOCH);
    private final AtomicReference<Instant> lastSuccessfulDrain = new AtomicReference<>(Instant.EPOCH);

    public AuditSpoolDrainer(
            AuditSpoolWriter spoolWriter,
            AuditService auditService,
            DatabaseWritableProbe probe
    ) {
        this.spoolWriter = spoolWriter;
        this.auditService = auditService;
        this.probe = probe;
    }

    public Instant getLastDrainAttempt() {
        return this.lastDrainAttempt.get();
    }

    public Instant getLastSuccessfulDrain() {
        return this.lastSuccessfulDrain.get();
    }

    /**
     * Best-effort flush of pending spool events before the application context is destroyed.
     * If the primary DB is offline, events remain on disk for the next startup drain.
     */
    @PreDestroy
    void drainOnShutdown() {
        int pending = this.spoolWriter.pendingCount();
        if (pending == 0) {
            return;
        }

        LOGGER.info("Shutting down: attempting to drain {} pending audit event(s) to the database", pending);
        int synced = this.drain();
        int remaining = this.spoolWriter.pendingCount();
        if (remaining > 0) {
            LOGGER.warn(
                    "Shutdown drain left {} audit event(s) on disk (synced {}); they will retry on next startup",
                    remaining,
                    synced
            );
        }
    }

    @Scheduled(fixedDelayString = "${audit.drain.fixed-delay-ms:30000}")
    public int drain() {
        this.lastDrainAttempt.set(Instant.now());

        if (!this.probe.isWritable()) {
            this.probe.probe();
            if (!this.probe.isWritable()) {
                return 0;
            }
        }

        List<AuditEventPayload> pending = this.spoolWriter.readPending();
        if (pending.isEmpty()) {
            return 0;
        }

        Set<UUID> synced = new HashSet<>();
        for (AuditEventPayload payload : pending) {
            if (this.auditService.trySaveToDatabase(payload)) {
                this.spoolWriter.appendArchive(payload);
                synced.add(payload.id());
            }
        }

        if (!synced.isEmpty()) {
            this.spoolWriter.removeByIds(synced);
            this.probe.markWriteSucceeded();
            this.lastSuccessfulDrain.set(Instant.now());
            LOGGER.info("Drained {} audit event(s) from spool to database", synced.size());
        }

        return synced.size();
    }
}
