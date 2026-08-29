package com.openrecordsmanager.audit.spool;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.audit.AuditEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class AuditSpoolWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditSpoolWriter.class);

    private final JsonMapper jsonMapper;
    private final AuditSpoolPaths paths;

    public AuditSpoolWriter(JsonMapper jsonMapper, AuditSpoolPaths paths) {
        this.jsonMapper = jsonMapper;
        this.paths = paths;
    }

    public void append(AuditEventPayload payload) {
        this.paths.ensureDirectoryExists();
        Path pending = this.paths.pendingFile();
        String line = this.jsonMapper.writeValueAsString(payload) + System.lineSeparator();

        synchronized (this.paths.lock()) {
            try (FileChannel channel = FileChannel.open(
                    pending,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            )) {
                channel.write(StandardCharsets.UTF_8.encode(line));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to append audit event to spool", e);
            }
        }
    }

    public List<AuditEventPayload> readPending() {
        Path pending = this.paths.pendingFile();
        if (!Files.exists(pending)) {
            return List.of();
        }

        synchronized (this.paths.lock()) {
            try {
                return Files.readAllLines(pending, StandardCharsets.UTF_8).stream()
                        .filter(line -> !line.isBlank())
                        .map(this::parseLine)
                        .toList();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read audit spool", e);
            }
        }
    }

    public List<AuditEventPayload> readForTarget(AuditEntityType targetType, String targetId) {
        return readPending().stream()
                .filter(event -> event.targetType() == targetType && event.targetId().equals(targetId))
                .toList();
    }

    public int pendingCount() {
        Path pending = this.paths.pendingFile();
        if (!Files.exists(pending)) {
            return 0;
        }

        synchronized (this.paths.lock()) {
            try {
                return (int) Files.lines(pending).filter(line -> !line.isBlank()).count();
            } catch (IOException e) {
                LOGGER.warn("Failed to count pending audit spool entries", e);
                return 0;
            }
        }
    }

    public void removeByIds(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }

        Path pending = this.paths.pendingFile();
        synchronized (this.paths.lock()) {
            if (!Files.exists(pending)) {
                return;
            }

            try {
                List<String> remaining = new ArrayList<>();
                for (String line : Files.readAllLines(pending, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) {
                        continue;
                    }
                    AuditEventPayload payload = parseLine(line);
                    if (!ids.contains(payload.id())) {
                        remaining.add(line);
                    }
                }
                Files.write(pending, remaining, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to update audit spool", e);
            }
        }
    }

    public void appendArchive(AuditEventPayload payload) {
        if (!this.paths.isArchiveEnabled()) {
            return;
        }

        this.paths.ensureDirectoryExists();
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Path archive = this.paths.directory().resolve("audit-" + date + ".log");
        String line = this.jsonMapper.writeValueAsString(payload) + System.lineSeparator();

        synchronized (this.paths.lock()) {
            try (FileChannel channel = FileChannel.open(
                    archive,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            )) {
                channel.write(StandardCharsets.UTF_8.encode(line));
            } catch (IOException e) {
                LOGGER.warn("Failed to append audit archive log entry for {}", payload.id(), e);
            }
        }
    }

    private AuditEventPayload parseLine(String line) {
        try {
            return this.jsonMapper.readValue(line, AuditEventPayload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid audit spool line: " + line, e);
        }
    }
}
