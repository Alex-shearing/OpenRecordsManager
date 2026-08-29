package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.audit.AuditEntityType;

public record AuditRelationship(
        AuditEntityType type,
        String id,
        String role
) {
}
