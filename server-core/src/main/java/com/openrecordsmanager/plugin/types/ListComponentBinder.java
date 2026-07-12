package com.openrecordsmanager.plugin.types;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.ListDefinition;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.plugin.ComponentCatalog;
import com.openrecordsmanager.plugin.ExpressionsService;

import java.util.Optional;

public class ListComponentBinder extends ComponentBinder<ListDefinition, ListType> {

    @Override
    public void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            ListDefinition definition
    ) {
        ListType type = new ListType(id, definition.name());
        repository.listTypeRepo.saveAndFlush(type);

        definition.defaultEntries().forEach((s, listItem) -> {
            ResourceIdentifier childId = new ResourceIdentifier(id.source(), s);

            ComponentBinderRegistry.LIST_ELEMENT.register(repository, catalog, expressions, childId, listItem, false);
        });

    }

    @Override
    public Optional<ListType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.listTypeRepo.findById(id);
    }
}
