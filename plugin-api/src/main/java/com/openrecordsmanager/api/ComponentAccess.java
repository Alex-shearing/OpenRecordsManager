package com.openrecordsmanager.api;

import com.openrecordsmanager.api.types.ComponentType;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public interface ComponentAccess {
    <T extends Component> Optional<T> getComponent(ComponentType<T> type, ResourceIdentifier id);

    <T extends Component> @Nullable ResourceIdentifier getId(ComponentType<T> type, T definition);
}
