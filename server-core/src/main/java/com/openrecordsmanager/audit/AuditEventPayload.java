package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AuditEventPayload(
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
    public AuditEventPayload {
        metadata = metadata == null ? null : Map.copyOf(metadata);
        changes = changes == null ? null : List.copyOf(changes);
        relationships = relationships == null ? null : List.copyOf(relationships);
    }
}
