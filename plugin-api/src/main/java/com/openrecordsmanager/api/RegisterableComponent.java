package com.openrecordsmanager.api;

import java.util.Set;

public interface RegisterableComponent {
    String id();

    default Set<RegisterableComponent> getDependencies() {
        return Set.of();
    }
}
