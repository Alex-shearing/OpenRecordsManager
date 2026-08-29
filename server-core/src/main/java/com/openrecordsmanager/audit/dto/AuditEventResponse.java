package com.openrecordsmanager.audit.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.audit.AuditEventPayload;
import com.openrecordsmanager.audit.AuditPropertyChange;
import com.openrecordsmanager.audit.AuditRelationship;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        Instant occurredAt,
        @Nullable UUID actorId,
        @Nullable String actorUsername,
        AuditOperation operation,
        AuditEntityType targetType,
        String targetId,
        @Nullable ResourceIdentifier actionId,
        String summary,
        @Nullable List<AuditPropertyChange> changes,
        @Nullable List<AuditRelationship> relationships,
        @Nullable String comment,
        @Nullable Map<String, Object> metadata
) {
    public static AuditEventResponse of(AuditEventPayload payload) {
        return new AuditEventResponse(
                payload.id(),
                payload.occurredAt(),
                payload.actorId(),
                payload.actorUsername(),
                payload.operation(),
                payload.targetType(),
                payload.targetId(),
                payload.actionId(),
                payload.summary(),
                payload.changes(),
                payload.relationships(),
                payload.comment(),
                payload.metadata()
        );
    }
}
