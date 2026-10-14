package com.openrecordsmanager.api;

import com.openrecordsmanager.api.types.ComponentType;

import java.util.Optional;

public interface ComponentAccess {

    <T extends Component> RegistryAccess<T> getRegistry(ComponentType<T> type);

    default <T extends Component> Optional<T> get(ComponentReference<T> reference) {
        return this.getRegistry(reference.type()).get(reference.id());
    }

    interface RegistryAccess<T extends Component> {
        Optional<T> get(ResourceIdentifier id);

        Optional<ResourceIdentifier> getId(T definition);
    }
}
