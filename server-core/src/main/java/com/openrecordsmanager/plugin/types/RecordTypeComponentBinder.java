package com.openrecordsmanager.plugin.types;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.recordtype.RecordTypeTemplate;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordTypeComponentBinder extends ComponentBinder<RecordTypeTemplate, RecordType> {

    @Override
    public void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            RecordTypeTemplate definition
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
            Map.Entry<ComponentReference<ObjectPropertyTemplate<?>>, ?> entry,
            ComponentCatalog catalog,
            DataRepository repository
    ) {
        ResourceIdentifier id = entry.getKey().getId(catalog)
                .orElseThrow(() -> new IllegalArgumentException("id " + entry.getKey() + " is not found"));
        ObjectProperty<?> property = (ObjectProperty<?>) catalog.getTemplateRegistry(ComponentTypes.OBJECT_PROPERTY).getRegistered(id, repository)
                .orElseThrow(() -> new IllegalArgumentException("Attempted to use property that was not registered: " + entry.getKey().getId(catalog)));

        return new RecordTypeProperty<>((ObjectProperty<T>) property, (T) entry.getValue());
    }

    @Override
    public Optional<RecordType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.recordTypeRepo.findById(id);
    }
}
