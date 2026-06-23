package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.model.RecordType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Optional;

public class RecordTypeType extends ResourceType<RecordTypeDefinition, RecordType> {
    public RecordTypeType() {
        super("record_type", RecordTypeDefinition.class);
    }

    @Override
    public RecordType register(DataRepository repository, ResourceCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, RecordTypeDefinition definition) {
        RecordType type = new RecordType(id, catalog, expressions, repository, definition);
        return repository.recordTypeRepo.saveAndFlush(type);
    }

    @Override
    public Optional<RecordType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.recordTypeRepo.findById(id);
    }
}
