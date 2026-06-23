package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Optional;

public class UnregisterableResourceType<T extends Component> extends ResourceType<T, Void> {
    public UnregisterableResourceType(String name, Class<T> componentClass) {
        super(name, componentClass);
    }

    @Deprecated
    @Override
    public Void register(DataRepository repository, ResourceCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, T definition) {
        throw new UnsupportedOperationException("This resource can not be registered.");
    }

    @Deprecated
    @Override
    public Optional<Void> getRegistered(ResourceIdentifier id, DataRepository repo) {
        throw new UnsupportedOperationException("This resource can not be registered.");
    }
}
