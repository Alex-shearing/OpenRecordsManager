package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;

import java.util.Optional;

public class ListResourceType extends ResourceType<ListDefinition, ListType> {
    public ListResourceType() {
        super("list", ListDefinition.class);
    }

    @Override
    public ListType register(DataRepository repository, ResourceRegistry registry, ExpressionsService expressions, ResourceIdentifier id, ListDefinition definition) {
        ListType type = new ListType(id, definition);
        return repository.listTypeRepo.saveAndFlush(type);
    }

    @Override
    protected Optional<ListType> get(ResourceIdentifier id, DataRepository repo) {
        return repo.listTypeRepo.findById(id);
    }
}
