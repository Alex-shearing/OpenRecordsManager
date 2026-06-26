package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.list.ListElementDefinition;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Optional;

public class ListElementComponentType extends ComponentType<ListElementDefinition, ListElement> {
    public ListElementComponentType() {
        super("list_elements", ListElementDefinition.class);
    }

    @Override
    public ListElement register(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, ListElementDefinition definition) {
        ResourceIdentifier parentId = catalog.getId(ComponentTypes.LIST, definition.parent());
        if (parentId == null) {
            throw new IllegalArgumentException("attempted to register list element to a parent that was not registered");
        }

        ListType parent = repository.listTypeRepo.findById(parentId).orElseThrow();

        ListElement type = new ListElement(
                id,
                parent,
                definition.display(),
                definition.description(),
                definition.index(),
                definition.activeTo(),
                definition.aliases()
        );
        return repository.listElementRepo.saveAndFlush(type);
    }

    @Override
    public Optional<ListElement> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.listElementRepo.findById(id);
    }
}
