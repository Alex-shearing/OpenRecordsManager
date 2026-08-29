package com.openrecordsmanager.audit.dto;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.audit.persistence.AuditPolicyEntity;

public record AuditPolicyResponse(
        AuditEntityType entityType,
        AuditOperation operation,
        boolean enabled,
        boolean requiresComment,
        String displayName,
        String description
) {
    public static AuditPolicyResponse of(AuditPolicyEntity entity) {
        return new AuditPolicyResponse(
                entity.entityType(),
                entity.operation(),
                entity.enabled,
                entity.requiresComment,
                entity.displayName,
                entity.description == null ? "" : entity.description
        );
    }
}
