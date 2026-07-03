package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;

import java.util.Optional;

public class ListComponentType extends ComponentBinding<ListDefinition, ListType> {

    @Override
    public void register(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, ListDefinition definition) {
        ListType type = new ListType(id, definition.display);
        repository.listTypeRepo.saveAndFlush(type);

        definition.defaultEntries.forEach((s, listItem) -> {
            ResourceIdentifier childId = new ResourceIdentifier(id.source(), s);

            ComponentBinderRegistry.LIST_ELEMENT.register(repository, catalog, expressions, childId, listItem, false);
        });

    }

    @Override
    public Optional<ListType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.listTypeRepo.findById(id);
    }
}
