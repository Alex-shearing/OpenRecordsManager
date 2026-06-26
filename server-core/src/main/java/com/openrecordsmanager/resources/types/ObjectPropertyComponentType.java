package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Optional;

public class ObjectPropertyComponentType extends ComponentType<PropertyDefinition<?>, ObjectProperty<?>> {
    @SuppressWarnings("unchecked")
    public ObjectPropertyComponentType() {
        super("object_properties", (Class<PropertyDefinition<?>>) (Class<?>) PropertyDefinition.class);
    }

    @Override
    protected ObjectProperty<?> register(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, PropertyDefinition<?> definition) {
        return this.registerInternal(repository, catalog, expressions, id, definition);
    }

    private <T> ObjectProperty<?> registerInternal(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, PropertyDefinition<T> definition) {
        ListType listType = null;
        if (definition.getType().allowsList() && definition.getListType() != null) {
            ResourceIdentifier listId = catalog.getId(ComponentTypes.LIST, definition.getListType());
            if (listId == null) {
                throw new IllegalArgumentException("ListType " + definition.getListType().id() + " does not exist");
            }
            listType = repository.listTypeRepo.findById(listId).orElseThrow();
        }

        ObjectProperty<?> type = new ObjectProperty<>(
                id,
                definition.getName(),
                definition.getDescription(),
                definition.getType(),
                listType,
                expressions.buildExpression(definition.getValidator()),
                expressions.buildExpression(definition.getSecurityFilter()),
                definition.getDefaultValue()
        );

        return repository.objectPropertyRepo.saveAndFlush(type);
    }

    @Override
    public Optional<ObjectProperty<?>> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.objectPropertyRepo.findById(id);
    }
}
