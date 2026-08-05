package com.openrecordsmanager.plugin.types;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.ListElementTemplate;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;

import java.util.Optional;

public class ListElementComponentBinder extends ComponentBinder<ListElementTemplate, ListElement> {

    @Override
    public void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            ListElementTemplate definition
    ) {
        ResourceIdentifier parentId = definition.parent().getId(catalog)
                .orElseThrow(() -> new IllegalArgumentException("attempted to register list element to a parent that was not registered"));

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
