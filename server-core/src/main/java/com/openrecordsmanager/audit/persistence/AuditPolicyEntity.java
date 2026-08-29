package com.openrecordsmanager.audit.persistence;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "audit_policy")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class AuditPolicyEntity {

    @EmbeddedId
    public AuditPolicyId id;

    @Column(nullable = false)
    public boolean enabled;

    @Column(name = "requires_comment", nullable = false)
    public boolean requiresComment;

    @Column(name = "display_name", nullable = false)
    public String displayName;

    @Column(length = 1000)
    @Nullable
    public String description;

    @Deprecated
    protected AuditPolicyEntity() {
    }

    public AuditPolicyEntity(
            AuditPolicyId id,
            boolean enabled,
            boolean requiresComment,
            String displayName,
            @Nullable String description
    ) {
        this.id = id;
        this.enabled = enabled;
        this.requiresComment = requiresComment;
        this.displayName = displayName;
        this.description = description;
    }

    public AuditEntityType entityType() {
        return this.id.entityType();
    }

    public AuditOperation operation() {
        return this.id.operation;
    }
}
