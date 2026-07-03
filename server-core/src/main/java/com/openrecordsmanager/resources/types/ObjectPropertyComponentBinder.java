package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.PropertyDefinition;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;

import java.util.Optional;

public class ObjectPropertyComponentBinder extends ComponentBinder<PropertyDefinition<?>, ObjectProperty<?>> {

    @Override
    protected void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            PropertyDefinition<?> definition
    ) {
        this.registerInternal(repository, catalog, expressions, id, definition);
    }

    private <T> void registerInternal(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            PropertyDefinition<T> definition
    ) {
        ListType listType = null;
        if (definition.getType().allowsList() && definition.getListType() != null) {
            listType = ComponentBinderRegistry.LIST.getRegistered(definition.getListType(), catalog, repository)
                    .orElse(null);
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

        repository.objectPropertyRepo.saveAndFlush(type);
    }

    @Override
    public Optional<ObjectProperty<?>> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.objectPropertyRepo.findById(id);
    }
}
