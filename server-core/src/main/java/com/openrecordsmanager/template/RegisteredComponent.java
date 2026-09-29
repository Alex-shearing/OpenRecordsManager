package com.openrecordsmanager.template;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentType;

public interface RegisteredComponent {
    ComponentType<?> getComponentType();

    ResourceIdentifier getId();

    default ComponentReference.Reference<?> getReference() {
        return ComponentReference.of(this.getComponentType(), this.getId());
    }
}
