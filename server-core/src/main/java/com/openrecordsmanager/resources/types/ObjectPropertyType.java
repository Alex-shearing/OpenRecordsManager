package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;

import java.util.Optional;

public class ObjectPropertyType extends ResourceType<PropertyDefinition<?>, ObjectProperty<?>> {
    public ObjectPropertyType() {
        super("object_property", (Class<PropertyDefinition<?>>) (Class<?>) PropertyDefinition.class);
    }

    @Override
    public ObjectProperty<?> register(DataRepository repository, ResourceRegistry registry, ExpressionsService expressions, ResourceIdentifier id, PropertyDefinition<?> definition) {
        ObjectProperty<?> type = new ObjectProperty<>(id, registry, expressions, repository, definition);
        return repository.objectPropertyRepo.saveAndFlush(type);
    }

    @Override
    protected Optional<ObjectProperty<?>> get(ResourceIdentifier id, DataRepository repo) {
        return repo.objectPropertyRepo.findById(id);
    }
}
