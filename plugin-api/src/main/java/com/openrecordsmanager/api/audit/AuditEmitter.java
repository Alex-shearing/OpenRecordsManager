package com.openrecordsmanager.api.audit;

import org.jspecify.annotations.Nullable;

public interface AuditEmitter {
    void addPropertyChangeEvent(String propertyId, @Nullable Object oldValue, @Nullable Object newValue);
}
