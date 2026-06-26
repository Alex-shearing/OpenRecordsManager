package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.RecordType;
import com.openrecordsmanager.model.RecordTypeProperty;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordTypeComponentType extends ComponentType<RecordTypeDefinition, RecordType> {
    public RecordTypeComponentType() {
        super("record_types", RecordTypeDefinition.class);
    }

    @Override
    public RecordType register(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, RecordTypeDefinition definition) {
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
        return repository.recordTypeRepo.saveAndFlush(type);
    }

    @SuppressWarnings("unchecked")
    private static <T> RecordTypeProperty<T> createRecordTypeProperty(Map.Entry<PropertyDefinition<?>, ?> entry, ComponentCatalog catalog, DataRepository repository) {
        ResourceIdentifier propId = catalog.getId(ComponentTypes.PROPERTY, entry.getKey());
        if (propId == null) {
            throw new IllegalArgumentException("Attempted to use property that was not in catalog: " + entry.getKey().getName());
        }

        Optional<ObjectProperty<?>> property = ComponentTypes.PROPERTY.getRegistered(propId, repository);
        if (property.isEmpty()) {
            throw new IllegalArgumentException("Attempted to use property that was not registered: " + propId);
        }

        return new RecordTypeProperty<>((ObjectProperty<T>) property.get(), (T) entry.getValue());
    }

    @Override
    public Optional<RecordType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.recordTypeRepo.findById(id);
    }
}
