package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Optional;

public class ObjectPropertyType extends ResourceType<PropertyDefinition<?>, ObjectProperty<?>> {
    @SuppressWarnings("unchecked")
    public ObjectPropertyType() {
        super("object_property", (Class<PropertyDefinition<?>>) (Class<?>) PropertyDefinition.class);
    }

    @Override
    public ObjectProperty<?> register(DataRepository repository, ResourceCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, PropertyDefinition<?> definition) {
        ObjectProperty<?> type = new ObjectProperty<>(id, catalog, expressions, repository, definition);
        return repository.objectPropertyRepo.saveAndFlush(type);
    }

    @Override
    public Optional<ObjectProperty<?>> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.objectPropertyRepo.findById(id);
    }
}
