package com.openrecordsmanager.audit.persistence;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "audit_policy")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class AuditPolicyEntity {

    @EmbeddedId
    private AuditPolicyId id;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "requires_comment", nullable = false)
    private boolean requiresComment;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(length = 1000)
    @Nullable
    private String description;

    @Column(nullable = false)
    private Instant dateModified;

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
        this.dateModified = Instant.now();
    }

    public AuditPolicyId getId() {
        return this.id;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.touchDateModified();
    }

    public boolean isRequiresComment() {
        return this.requiresComment;
    }

    public void setRequiresComment(boolean requiresComment) {
        this.requiresComment = requiresComment;
        this.touchDateModified();
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.touchDateModified();
    }

    public @Nullable String getDescription() {
        return this.description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
        this.touchDateModified();
    }

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
    }

    public AuditEntityType entityType() {
        return this.id.entityType();
    }

    public AuditOperation operation() {
        return this.id.operation;
    }
}
