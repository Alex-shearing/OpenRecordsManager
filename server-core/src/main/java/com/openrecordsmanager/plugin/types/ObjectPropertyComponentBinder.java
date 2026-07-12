package com.openrecordsmanager.plugin.types;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.PropertyDefinition;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.plugin.ComponentCatalog;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.ObjectProperty;

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
        if (definition.type().allowsList() && definition.listType() != null) {
            ResourceIdentifier listId = definition.listType().getId(catalog)
                    .orElseThrow(() -> new RuntimeException("listType " + definition.listType() + " does not exist"));

            listType = ComponentBinderRegistry.LIST.getRegistered(listId, repository)
                    .orElse(null);

            if (listType == null) {
                throw new IllegalArgumentException("listType " + definition.listType() + " is not registered");
            }
        }

        ObjectProperty<?> type = new ObjectProperty<>(
                id,
                definition.name(),
                definition.description(),
                definition.type(),
                listType,
                expressions.buildExpression(definition.validator()),
                expressions.buildExpression(definition.securityFilter()),
                definition.defaultValue()
        );

        repository.objectPropertyRepo.saveAndFlush(type);
    }

    @Override
    public Optional<ObjectProperty<?>> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.objectPropertyRepo.findById(id);
    }
}
