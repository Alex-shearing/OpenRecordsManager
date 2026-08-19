package com.openrecordsmanager.plugin.registry.mapper;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;

import java.util.Optional;

public class ObjectPropertyTemplateRegistrationMapper extends TemplateRegistrationMapper<ObjectPropertyTemplate<?>, ObjectProperty<?>> {

    @Override
    protected void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            ObjectPropertyTemplate<?> component
    ) {
        this.registerInternal(repository, catalog, expressions, id, component);
    }

    private <T> void registerInternal(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            ObjectPropertyTemplate<T> definition
    ) {
        ListType listType = null;
        if (definition.type().allowsList() && definition.listType() != null) {
            ResourceIdentifier listId = definition.listType().getId(catalog)
                    .orElseThrow(() -> new RuntimeException("listType " + definition.listType() + " does not exist"));

            listType = (ListType) catalog.getTemplateRegistry(ComponentTypes.LIST).getRegistered(listId, repository)
                    .orElseThrow(() -> new IllegalArgumentException("listType " + definition.listType() + " is not registered"));
        }

        ObjectProperty<?> type = new ObjectProperty<>(
                id,
                definition.name(),
                definition.description(),
                definition.type(),
                listType,
                expressions.buildExpression(definition.validator()),
                expressions.buildExpression(definition.securityFilter()),
                definition.defaultValue(),
                definition.userHidden()
        );

        repository.objectPropertyRepo.saveAndFlush(type);
    }

    @Override
    public Optional<ObjectProperty<?>> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.objectPropertyRepo.findById(id);
    }
}
