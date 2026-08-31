package com.openrecordsmanager.audit.spool;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.config.ConfigService;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AuditSpoolPaths {

    private final ConfigService config;
    private final AtomicReference<Object> lock = new AtomicReference<>(new Object());
    private final Path auditPath;

    public AuditSpoolPaths(ConfigService config) {
        this.config = config;
        this.auditPath = Path.of(this.config.getOrDefault(BuiltinConfigs.AUDIT_SPOOL_DIRECTORY, "./data/audit"));
    }

    public Object lock() {
        return this.lock.get();
    }

    public Path directory() {
        return this.auditPath;
    }

    public Path pendingFile() {
        return this.directory().resolve("pending.ndjson");
    }

    public boolean isArchiveEnabled() {
        return this.config.getOptional(BuiltinConfigs.AUDIT_FILE_ARCHIVE_ENABLED).orElse(true);
    }

    public void ensureDirectoryExists() {
        try {
            java.nio.file.Files.createDirectories(this.directory());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to create audit spool directory", e);
        }
    }
}
