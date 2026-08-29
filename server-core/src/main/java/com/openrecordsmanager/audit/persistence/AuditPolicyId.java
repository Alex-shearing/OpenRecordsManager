package com.openrecordsmanager.audit.persistence;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AuditPolicyId implements Serializable {

    @Column(name = "entity_type", nullable = false, length = 100)
    public String entityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 50)
    public AuditOperation operation;

    @Deprecated
    protected AuditPolicyId() {
    }

    public AuditPolicyId(AuditEntityType entityType, AuditOperation operation) {
        this.entityType = entityType.key();
        this.operation = operation;
    }

    public AuditEntityType entityType() {
        return AuditEntityType.fromKey(this.entityType);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuditPolicyId that = (AuditPolicyId) o;
        return Objects.equals(this.entityType, that.entityType) && this.operation == that.operation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entityType, this.operation);
    }
}
