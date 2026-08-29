package com.openrecordsmanager.audit.persistence;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class AuditEventEntity {

    @Id
    public UUID id;

    @Column(name = "occurred_at", nullable = false)
    public Instant occurredAt;

    @Column(name = "actor_id")
    @Nullable
    public UUID actorId;

    @Column(name = "actor_username")
    @Nullable
    public String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AuditOperation operation;

    @Column(name = "target_type", nullable = false)
    public String targetType;

    @Column(name = "target_id", nullable = false)
    public String targetId;

    @Column(name = "action_id")
    @JavaType(ResourceIdentifierJavaType.class)
    @Nullable
    public ResourceIdentifier actionId;

    @Column(nullable = false, length = 1000)
    public String summary;

    @Column(columnDefinition = "CLOB")
    @Nullable
    public String changes;

    @Column(columnDefinition = "CLOB")
    @Nullable
    public String relationships;

    @Column(length = 2000)
    @Nullable
    public String comment;

    @Column(columnDefinition = "CLOB")
    @Nullable
    public String metadata;

    @Deprecated
    protected AuditEventEntity() {
    }

    public AuditEventEntity(
            UUID id,
            Instant occurredAt,
            @Nullable UUID actorId,
            @Nullable String actorUsername,
            AuditOperation operation,
            String targetType,
            String targetId,
            @Nullable ResourceIdentifier actionId,
            String summary,
            @Nullable String changes,
            @Nullable String relationships,
            @Nullable String comment,
            @Nullable String metadata
    ) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.operation = operation;
        this.targetType = targetType;
        this.targetId = targetId;
        this.actionId = actionId;
        this.summary = summary;
        this.changes = changes;
        this.relationships = relationships;
        this.comment = comment;
        this.metadata = metadata;
    }
}
