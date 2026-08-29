package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.audit.persistence.AuditPolicyEntity;
import com.openrecordsmanager.audit.persistence.AuditPolicyId;
import com.openrecordsmanager.audit.spool.AuditSpoolDrainer;
import com.openrecordsmanager.audit.spool.AuditSpoolWriter;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.DatabaseWritableProbe;
import com.openrecordsmanager.list.ListService;
import com.openrecordsmanager.rest.errors.AuditCommentRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditIntegrationTest {

    private static Path spoolDirectory;

    @DynamicPropertySource
    static void auditProperties(DynamicPropertyRegistry registry) {
        spoolDirectory = Path.of("build/test-audit-" + UUID.randomUUID());
        registry.add(BuiltinConfigs.AUDIT_SPOOL_DIRECTORY.key(), () -> spoolDirectory.toString());
        registry.add(BuiltinConfigs.AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS.key(), () -> "60000");
        registry.add("audit.probe.interval-ms", () -> "60000");
    }

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditPolicyService auditPolicyService;

    @Autowired
    private AuditSpoolWriter spoolWriter;

    @Autowired
    private AuditSpoolDrainer drainer;

    @Autowired
    private DatabaseWritableProbe probe;

    @Autowired
    private DataRepository repository;

    @Autowired
    private ListService listService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetProbeAndSpool() throws java.io.IOException {
        this.probe.markWriteSucceeded();
        java.nio.file.Files.createDirectories(spoolDirectory);
        java.nio.file.Files.writeString(spoolDirectory.resolve("pending.ndjson"), "");
    }

    @Test
    void commentRequiredWhenPolicyEnabled() {
        this.auditPolicyService.ensurePolicyExists(
                AuditEntityType.RECORD,
                AuditOperation.CREATE,
                "Record created",
                "test"
        );
        this.repository.auditPolicyRepo.saveAndFlush(new AuditPolicyEntity(
                new AuditPolicyId(AuditEntityType.RECORD, AuditOperation.CREATE),
                true,
                true,
                "Record created",
                "test"
        ));

        AuditContext.begin(null, "tester", null, true);
        try {
            assertThrows(
                    AuditCommentRequiredException.class,
                    () -> this.auditPolicyService.validateCommentRequired(
                            AuditEntityType.RECORD,
                            AuditOperation.CREATE
                    )
            );
        } finally {
            AuditContext.clear();
        }

        AuditContext.begin(null, "tester", "approved change", true);
        try {
            assertDoesNotThrow(() -> this.auditPolicyService.validateCommentRequired(
                    AuditEntityType.RECORD,
                    AuditOperation.CREATE
            ));
        } finally {
            AuditContext.clear();
        }
    }

    @Test
    void aspectEnforcesCommentOnAnnotatedServiceMethod() {
        this.auditPolicyService.ensurePolicyExists(
                AuditEntityType.LIST,
                AuditOperation.CREATE,
                "List created",
                "test"
        );
        this.repository.auditPolicyRepo.saveAndFlush(new AuditPolicyEntity(
                new AuditPolicyId(AuditEntityType.LIST, AuditOperation.CREATE),
                true,
                true,
                "List created",
                "test"
        ));

        AuditContext.begin(null, "tester", null, true);
        try {
            assertThrows(
                    AuditCommentRequiredException.class,
                    () -> this.listService.create(new com.openrecordsmanager.list.dto.NewListTypeRequest(
                            com.openrecordsmanager.api.ResourceIdentifier.valueOf("test:audit_list"),
                            "Audit test list"
                    ))
            );
        } finally {
            AuditContext.clear();
        }
    }

    @Test
    void auditEventIsSpooledAndDrainedToDatabase() {
        AuditEventPayload payload = samplePayload(AuditEntityType.RECORD, UUID.randomUUID().toString());

        this.spoolWriter.append(payload);
        assertEquals(1, this.spoolWriter.pendingCount());
        assertFalse(this.repository.auditEventRepo.existsById(payload.id()));

        int drained = this.drainer.drain();

        assertEquals(1, drained);
        assertTrue(this.repository.auditEventRepo.existsById(payload.id()));
        assertEquals(0, this.spoolWriter.pendingCount());
    }

    @Test
    void auditSkipsDatabaseWhenPrimaryIsReadOnly() {
        AuditEventPayload payload = samplePayload(AuditEntityType.USER, UUID.randomUUID().toString());

        this.probe.markWriteFailed();
        this.auditService.persistImmediately(payload);

        assertEquals(1, this.spoolWriter.pendingCount());
        assertFalse(this.repository.auditEventRepo.existsById(payload.id()));
    }

    @Test
    void auditEventPublishedAfterTransactionCommit() {
        TransactionTemplate tx = new TransactionTemplate(this.transactionManager);

        tx.executeWithoutResult(status -> {
            AuditContext.begin(UUID.randomUUID(), "audit_tester", "commit test", true);
            try {
                this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.CONFIG, "audit.test.key");
            } finally {
                AuditContext.clear();
            }
        });

        List<AuditEventPayload> matching = this.repository.auditEventRepo.findAll().stream()
                .filter(event -> "audit.test.key".equals(event.targetId))
                .map(this.auditService::toPayload)
                .toList();
        assertFalse(matching.isEmpty());
    }

    @Test
    void entityHistoryMergesSpooledEvents() {
        String targetId = UUID.randomUUID().toString();
        AuditEventPayload spooled = samplePayload(AuditEntityType.RECORD, targetId);

        this.probe.markWriteFailed();
        this.auditService.persistImmediately(spooled);

        List<AuditEventPayload> pending = this.spoolWriter.readForTarget(AuditEntityType.RECORD, targetId);
        assertEquals(1, pending.size());
        assertEquals(spooled.id(), pending.getFirst().id());
    }

    @Test
    void readEventIsRecorded() {
        this.probe.markWriteFailed();
        TransactionTemplate tx = new TransactionTemplate(this.transactionManager);

        tx.executeWithoutResult(status -> {
            AuditContext.begin(UUID.randomUUID(), "reader", null, true);
            try {
                this.auditService.addReadEvent(AuditEntityType.RECORD, UUID.randomUUID().toString());
            } finally {
                AuditContext.clear();
            }
        });

        List<AuditEventPayload> reads = this.spoolWriter.readPending().stream()
                .filter(event -> event.operation() == AuditOperation.READ)
                .toList();
        assertEquals(1, reads.size());
        assertEquals(AuditEntityType.RECORD, reads.getFirst().targetType());
    }

    private static AuditEventPayload samplePayload(AuditEntityType targetType, String targetId) {
        return new AuditEventPayload(
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                "tester",
                AuditOperation.CREATE,
                targetType,
                targetId,
                null,
                "Created " + targetType.key() + " " + targetId,
                null,
                null,
                null,
                null
        );
    }
}
