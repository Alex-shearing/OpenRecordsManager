package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Optional;

public class ListComponentType extends TemplateComponentType<ListDefinition, ListType> {
    public ListComponentType(String name) {
        super(name, ListDefinition.class);
    }

    @Override
    public void register(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, ListDefinition definition) {
        ListType type = new ListType(id, definition.display);
        repository.listTypeRepo.saveAndFlush(type);

        definition.defaultEntries.forEach((s, listItem) -> {
            ResourceIdentifier childId = new ResourceIdentifier(id.source(), s);
            ComponentTypes.LIST_ELEMENT.register(repository, catalog, expressions, childId, listItem, false);
        });

    }

    @Override
    public Optional<ListType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.listTypeRepo.findById(id);
    }
}
