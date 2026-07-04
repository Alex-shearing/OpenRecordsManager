package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.PropertyDefinition;
import com.openrecordsmanager.api.template.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.RecordType;
import com.openrecordsmanager.model.RecordTypeProperty;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordTypeComponentBinder extends ComponentBinder<RecordTypeDefinition, RecordType> {

    @Override
    public void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            RecordTypeDefinition definition
    ) {
        Set<RecordTypeProperty<?>> properties = definition.properties().entrySet()
                .stream()
                .map(def -> createRecordTypeProperty(def, catalog, repository))
                .collect(Collectors.<RecordTypeProperty<?>>toSet());

        RecordType type = new RecordType(
                id,
                definition.name(),
                definition.description(),
                definition.allowedContentTypes(),
                expressions.buildExpression(definition.securityFilter()),
                definition.securityFilterUsage(),
                properties
        );
        repository.recordTypeRepo.saveAndFlush(type);
    }

    @SuppressWarnings("unchecked")
    private static <T> RecordTypeProperty<T> createRecordTypeProperty(
            Map.Entry<ComponentReference<PropertyDefinition<?>>, ?> entry,
            ComponentCatalog catalog,
            DataRepository repository
    ) {
        Optional<ObjectProperty<?>> property = ComponentBinderRegistry.PROPERTY.getRegistered(entry.getKey(), catalog, repository);
        if (property.isEmpty()) {
            throw new IllegalArgumentException("Attempted to use property that was not registered: " + entry.getKey().getId(catalog));
        }

        return new RecordTypeProperty<>((ObjectProperty<T>) property.get(), (T) entry.getValue());
    }

    @Override
    public Optional<RecordType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.recordTypeRepo.findById(id);
    }
}
