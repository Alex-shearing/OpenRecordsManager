package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.RegisterableComponent;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;

import java.util.Optional;

public class UnregisterableResourceType<T extends RegisterableComponent> extends ResourceType<T, Void> {
    public UnregisterableResourceType(String name, Class<T> componentClass) {
        super(name, componentClass);
    }

    @Deprecated
    @Override
    public Void register(DataRepository repository, ResourceRegistry registry, ExpressionsService expressions, ResourceIdentifier id, T definition) {
        throw new UnsupportedOperationException("This resource can not be registered.");
    }

    @Deprecated
    @Override
    protected Optional<Void> get(ResourceIdentifier id, DataRepository repo) {
        throw new UnsupportedOperationException("This resource can not be registered.");
    }
}
