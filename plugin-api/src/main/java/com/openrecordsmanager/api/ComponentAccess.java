package com.openrecordsmanager.api;

import com.openrecordsmanager.api.types.ComponentType;

import java.util.Optional;

public interface ComponentAccess {
    <T extends Component> Optional<T> getComponent(ComponentType<T> type, ResourceIdentifier id);

    <T extends Component> ResourceIdentifier getId(ComponentType<T> type, T definition);
}
