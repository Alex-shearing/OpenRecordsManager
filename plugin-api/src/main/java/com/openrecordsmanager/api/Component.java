package com.openrecordsmanager.api;

import java.util.Set;

public interface Component {
    String id();

    default Set<Component> getDependencies() {
        return Set.of();
    }
}
