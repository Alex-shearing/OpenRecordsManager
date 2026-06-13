package com.openrecordsmanager.resources;

import com.openrecordsmanager.RegisterableComponent;
import com.openrecordsmanager.auth.InputAuthProviderType;
import com.openrecordsmanager.auth.RedirectAuthProviderType;
import com.openrecordsmanager.list.ListDefinition;
import com.openrecordsmanager.property.PropertyDefinition;
import com.openrecordsmanager.recordtype.RecordTypeDefinition;

public enum ResourceType {
    LIST(ListDefinition.class),
    PROPERTY(PropertyDefinition.class),
    RECORD_TYPE(RecordTypeDefinition.class),
    INPUT_AUTH_PROVIDER(InputAuthProviderType.class),
    REDIRECT_AUTH_PROVIDER(RedirectAuthProviderType.class),
    ;

    private final Class<? extends RegisterableComponent> componentClass;

    ResourceType(Class<? extends RegisterableComponent> componentClass) {
        this.componentClass = componentClass;
    }

    public boolean isOf(RegisterableComponent component) {
        return this.componentClass.isInstance(component);
    }
}
