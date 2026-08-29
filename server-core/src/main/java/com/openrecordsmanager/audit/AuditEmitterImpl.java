package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.audit.AuditEmitter;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class AuditEmitterImpl implements AuditEmitter {

    private final AuditService auditService;
    private final AuditEntityType targetType;
    private final String targetId;

    public AuditEmitterImpl(AuditService auditService, AuditEntityType targetType, String targetId) {
        this.auditService = auditService;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    @Override
    public void addPropertyChangeEvent(String propertyId, @Nullable Object oldValue, @Nullable Object newValue) {
        this.auditService.addEvent(
                AuditOperation.UPDATE,
                this.targetType,
                this.targetId,
                List.of(new AuditPropertyChange(propertyId, oldValue, newValue)),
                null,
                null
        );
    }
}
