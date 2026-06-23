package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.list.ListElementDefinition;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Optional;

public class ListElementResourceType extends ResourceType<ListElementDefinition, ListElement> {
    public ListElementResourceType() {
        super("list_element", ListElementDefinition.class);
    }

    @Override
    public ListElement register(DataRepository repository, ResourceCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, ListElementDefinition definition) {
        ResourceIdentifier parentId = catalog.getResourceId(ResourceTypes.LIST, definition.parent());
        if (parentId == null) {
            throw new IllegalArgumentException("attempted to register list element to a parent that was not registered");
        }

        ListType parent = repository.listTypeRepo.findById(parentId).orElseThrow();

        ListElement type = new ListElement(id, parent, definition);
        return repository.listElementRepo.saveAndFlush(type);
    }

    @Override
    public Optional<ListElement> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.listElementRepo.findById(id);
    }
}
