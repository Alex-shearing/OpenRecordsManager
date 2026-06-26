package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Optional;

/**
 * Used to represent components that are supplied by plugins, but cannot be registered to the database.
 *
 * @param <T> the component class
 */
public class UnregisterableComponentType<T extends Component> extends ComponentType<T, Void> {
    public UnregisterableComponentType(String name, Class<T> componentClass) {
        super(name, componentClass);
    }

    @Deprecated
    @Override
    public Void register(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, T definition) {
        throw new UnsupportedOperationException("This resource can not be registered.");
    }

    @Deprecated
    @Override
    public Optional<Void> getRegistered(ResourceIdentifier id, DataRepository repo) {
        throw new UnsupportedOperationException("This resource can not be registered.");
    }
}
