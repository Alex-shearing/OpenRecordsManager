package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.audit.dto.AuditStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditStatusIntegrationTest {

    private static Path spoolDirectory;

    @DynamicPropertySource
    static void auditProperties(DynamicPropertyRegistry registry) {
        spoolDirectory = Path.of("build/test-audit-status-" + UUID.randomUUID());
        com.openrecordsmanager.database.SqliteTestSupport.registerPrimaryMemoryDatabase(registry, AuditStatusIntegrationTest.class);
        registry.add(BuiltinConfigs.AUDIT_SPOOL_DIRECTORY.key(), () -> spoolDirectory.toString());
        registry.add(BuiltinConfigs.AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS.key(), () -> "45");
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add("audit.probe.interval-ms", () -> "60000");
    }

    @Autowired
    private AuditQueryService queryService;

    @BeforeEach
    void resetSpool() throws java.io.IOException {
        java.nio.file.Files.createDirectories(spoolDirectory);
        java.nio.file.Files.writeString(spoolDirectory.resolve("pending.ndjson"), "");
    }

    @Test
    void getAuditStatusReturnsEnrichedFieldsWhenEnabled() {
        AuditStatusResponse status = this.queryService.getAuditStatus();

        assertTrue(status.auditEnabled());
        assertNull(status.auditDisabledReason());
        assertTrue(status.primaryWritable());
        assertEquals(0, status.pendingSpoolCount());
        assertTrue(status.archiveEnabled());
        assertEquals(45, status.drainIntervalSeconds());
        assertNotNull(status.lastProbeAt());
    }
}
