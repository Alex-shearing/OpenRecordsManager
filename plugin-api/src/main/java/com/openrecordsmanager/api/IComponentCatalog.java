package com.openrecordsmanager.api;

import com.openrecordsmanager.api.types.ComponentType;

public interface IComponentCatalog {
    <T extends Component> T getComponent(ComponentType<T> type, ResourceIdentifier id);
}
