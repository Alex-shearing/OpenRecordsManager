package com.openrecordsmanager.audit;

import org.jspecify.annotations.Nullable;

public record AuditPropertyChange(
        String property,
        @Nullable Object oldValue,
        @Nullable Object newValue
) {
}
