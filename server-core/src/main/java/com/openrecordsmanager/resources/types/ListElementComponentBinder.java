package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.ListElementDefinition;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;

import java.util.Optional;

public class ListElementComponentBinder extends ComponentBinder<ListElementDefinition, ListElement> {

    @Override
    public void register(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, ListElementDefinition definition) {
        ResourceIdentifier parentId = definition.parent().getId(catalog);
        if (parentId == null) {
            throw new IllegalArgumentException("attempted to register list element to a parent that was not registered");
        }

        ListType parent = repository.listTypeRepo.findById(parentId).orElseThrow();

        ListElement type = new ListElement(
                id,
                parent,
                definition.name(),
                definition.description(),
                definition.index(),
                definition.activeTo(),
                definition.aliases()
        );
        repository.listElementRepo.saveAndFlush(type);
    }

    @Override
    public Optional<ListElement> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.listElementRepo.findById(id);
    }
}
