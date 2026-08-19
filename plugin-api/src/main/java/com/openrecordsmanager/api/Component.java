package com.openrecordsmanager.api;

import com.openrecordsmanager.api.template.TemplateComponent;

import java.util.Set;

public interface Component {
    default Set<ComponentReference<? extends TemplateComponent>> getDependencies() {
        return Set.of();
    }
}
