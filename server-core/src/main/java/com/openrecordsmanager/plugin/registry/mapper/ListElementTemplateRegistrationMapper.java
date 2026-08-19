package com.openrecordsmanager.plugin.registry.mapper;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.ListElementTemplate;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;

import java.util.Optional;

public class ListElementTemplateRegistrationMapper extends TemplateRegistrationMapper<ListElementTemplate, ListElement> {

    @Override
    public void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            ListElementTemplate component
    ) {
        ResourceIdentifier parentId = component.parent().getId(catalog)
                .orElseThrow(() -> new IllegalArgumentException("attempted to register list element to a parent that was not registered"));

        ListType parent = repository.listTypeRepo.findById(parentId).orElseThrow();

        ListElement type = new ListElement(
                id,
                parent,
                component.name(),
                component.description(),
                component.index(),
                component.activeTo(),
                component.aliases()
        );
        repository.listElementRepo.saveAndFlush(type);
    }

    @Override
    public Optional<ListElement> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.listElementRepo.findById(id);
    }
}
