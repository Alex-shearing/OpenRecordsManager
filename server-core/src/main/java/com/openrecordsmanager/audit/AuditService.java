package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.audit.persistence.AuditEventEntity;
import com.openrecordsmanager.audit.persistence.AuditEventRepository;
import com.openrecordsmanager.audit.spool.AuditSpoolWriter;
import com.openrecordsmanager.database.DatabaseWritableProbe;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuditService {

    public static final String COLLECTION_TARGET_ID = "*";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);

    private final ApplicationEventPublisher publisher;
    private final AuditPolicyService policyService;
    private final AuditSpoolWriter spoolWriter;
    private final DatabaseWritableProbe probe;
    private final JsonMapper jsonMapper;
    private final AuditEventRepository auditEventRepo;
    private final AuditService self;

    public AuditService(
            ApplicationEventPublisher publisher,
            AuditPolicyService policyService,
            AuditSpoolWriter spoolWriter,
            DatabaseWritableProbe probe,
            JsonMapper jsonMapper,
            AuditEventRepository auditEventRepo,
            @Lazy AuditService self
    ) {
        this.publisher = publisher;
        this.policyService = policyService;
        this.spoolWriter = spoolWriter;
        this.probe = probe;
        this.jsonMapper = jsonMapper;
        this.auditEventRepo = auditEventRepo;
        this.self = self;
    }

    public void addEvent(AuditOperation operation, AuditEntityType targetType, UUID targetId) {
        this.addEvent(operation, targetType, targetId.toString());
    }

    public void addEvent(AuditOperation operation, AuditEntityType targetType, ResourceIdentifier targetId) {
        this.addEvent(operation, targetType, targetId.toString());
    }

    public void addEvent(AuditOperation operation, AuditEntityType targetType, String targetId) {
        this.addEvent(
                operation,
                targetType,
                targetId,
                null,
                AuditEventDescriptions.summary(operation, targetType, targetId),
                null,
                null,
                null
        );
    }

    public void addEvent(
            AuditOperation operation,
            AuditEntityType targetType,
            String targetId,
            @Nullable List<AuditPropertyChange> changes,
            @Nullable List<AuditRelationship> relationships,
            @Nullable Map<String, Object> metadata
    ) {
        this.addEvent(
                operation,
                targetType,
                targetId,
                null,
                AuditEventDescriptions.summary(operation, targetType, targetId),
                changes,
                relationships,
                metadata
        );
    }

    public void addActionRanEvent(
            ResourceIdentifier actionId,
            AuditEntityType targetType,
            UUID targetId,
            @Nullable Map<String, Object> metadata
    ) {
        this.addEvent(
                AuditOperation.ACTION,
                targetType,
                targetId.toString(),
                actionId,
                "Executed " + actionId + " action on " + targetId,
                null,
                null,
                metadata
        );
    }

    public void addReadEvent(AuditEntityType targetType, UUID targetId) {
        this.addReadEvent(targetType, targetId.toString());
    }

    public void addReadEvent(AuditEntityType targetType, ResourceIdentifier targetId) {
        this.addReadEvent(targetType, targetId.toString());
    }

    public void addReadEvent(AuditEntityType targetType, String targetId) {
        this.addEvent(AuditOperation.READ, targetType, targetId);
    }

    public void addReadEvent(
            AuditEntityType targetType,
            String targetId,
            @Nullable List<AuditRelationship> relationships
    ) {
        this.addEvent(AuditOperation.READ, targetType, targetId, null, relationships, null);
    }

    public void recordCollectionRead(AuditEntityType targetType, int resultCount) {
        this.addEvent(
                AuditOperation.READ,
                targetType,
                COLLECTION_TARGET_ID,
                null,
                null,
                Map.of("resultCount", resultCount)
        );
    }

    public void recordSearchRead(
            AuditEntityType targetType,
            String scopeTargetId,
            String search,
            int resultCount
    ) {
        this.addEvent(
                AuditOperation.READ,
                targetType,
                scopeTargetId,
                null,
                null,
                Map.of("search", search, "resultCount", resultCount)
        );
    }

    protected void addEvent(
            AuditOperation operation,
            AuditEntityType targetType,
            String targetId,
            @Nullable ResourceIdentifier actionId,
            String summary,
            @Nullable List<AuditPropertyChange> changes,
            @Nullable List<AuditRelationship> relationships,
            @Nullable Map<String, Object> metadata
    ) {
        if (!AuditContext.isCaptureEnabled() || !this.policyService.isEventEnabled(targetType, operation)) {
            return;
        }
        AuditEventPayload payload = new AuditEventPayload(
                UUID.randomUUID(),
                Instant.now(),
                AuditContext.actorId().orElse(null),
                AuditContext.actorUsername().orElse(null),
                operation,
                targetType,
                targetId,
                actionId,
                summary,
                changes,
                relationships,
                AuditContext.comment().orElse(null),
                metadata
        );
        this.publisher.publishEvent(payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAuditRecordRequested(AuditEventPayload event) {
        this.persist(event);
    }

    public void persist(AuditEventPayload payload) {
        LOGGER.debug("New audit event: {}", payload);

        this.spoolWriter.append(payload);

        if (this.trySaveToDatabase(payload)) {
            this.spoolWriter.removeByIds(Set.of(payload.id()));
            this.spoolWriter.appendArchive(payload);
            this.probe.markWriteSucceeded();
        }
    }

    public boolean trySaveToDatabase(AuditEventPayload payload) {
        if (!this.probe.isWritable()) {
            return false;
        }

        try {
            this.self.saveIfAbsent(this.toEntity(payload));
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to persist audit event {} to database", payload.id(), e);
            this.probe.markWriteFailed();
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIfAbsent(AuditEventEntity entity) {
        if (this.auditEventRepo.existsById(entity.id)) {
            return;
        }
        this.auditEventRepo.saveAndFlush(entity);
    }

    public AuditEventEntity toEntity(AuditEventPayload payload) {
        return new AuditEventEntity(
                payload.id(),
                payload.occurredAt(),
                payload.actorId(),
                payload.actorUsername(),
                payload.operation(),
                payload.targetType().key(),
                payload.targetId(),
                payload.actionId(),
                payload.summary(),
                serializeJson(payload.changes()),
                serializeJson(payload.relationships()),
                payload.comment(),
                serializeJson(payload.metadata())
        );
    }

    public AuditEventPayload toPayload(AuditEventEntity entity) {
        AuditEntityType type = AuditEntityType.fromKey(entity.targetType);

        return new AuditEventPayload(
                entity.id,
                entity.occurredAt,
                entity.actorId,
                entity.actorUsername,
                entity.operation,
                type,
                entity.targetId,
                entity.actionId,
                entity.summary,
                this.deserializeChanges(entity.changes),
                this.deserializeRelationships(entity.relationships),
                entity.comment,
                this.deserializeMetadata(entity.metadata)
        );
    }

    private @Nullable String serializeJson(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        return this.jsonMapper.writeValueAsString(value);
    }

    private @Nullable List<AuditPropertyChange> deserializeChanges(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return this.jsonMapper.readValue(
                json,
                this.jsonMapper.getTypeFactory().constructCollectionType(List.class, AuditPropertyChange.class)
        );
    }

    private @Nullable List<AuditRelationship> deserializeRelationships(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return this.jsonMapper.readValue(
                json,
                this.jsonMapper.getTypeFactory().constructCollectionType(List.class, AuditRelationship.class)
        );
    }

    private @Nullable Map<String, Object> deserializeMetadata(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return this.jsonMapper.readValue(
                json,
                this.jsonMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
        );
    }

    public record AuditRecordRequested(AuditEventPayload payload) {
    }
}
