package com.openrecordsmanager.plugin.registry.mapper;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.recordtype.PropertyAssignment;
import com.openrecordsmanager.api.template.recordtype.RecordTypeTemplate;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordTypeTemplateRegistrationMapper extends TemplateRegistrationMapper<RecordTypeTemplate, RecordType> {

    @Override
    public ComponentType<RecordTypeTemplate> componentType() {
        return ComponentTypes.RECORD_TYPE;
    }

    @Override
    public void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            ResourceIdentifier id,
            RecordTypeTemplate component
    ) {
        Set<RecordTypeProperty<?>> properties = component.properties()
                .stream()
                .map(def -> createRecordTypeProperty(def, catalog, repository))
                .collect(Collectors.<RecordTypeProperty<?>>toSet());

        RecordType type = new RecordType(
                id,
                component.name(),
                component.description(),
                component.allowedContentTypes(),
                expressions.buildExpression(component.securityFilter()),
                component.securityFilterUsage(),
                properties
        );
        repository.recordTypeRepo.saveAndFlush(type);
    }

    @SuppressWarnings("unchecked")
    private static <T> RecordTypeProperty<T> createRecordTypeProperty(
            PropertyAssignment<T> assignment,
            ComponentCatalog catalog,
            DataRepository repository
    ) {
        ResourceIdentifier id = assignment.property().getId(catalog)
                .orElseThrow(() -> new IllegalArgumentException("id " + assignment.property() + " is not found"));
        ObjectProperty<T> property = (ObjectProperty<T>) catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER).getRegistered(id, repository)
                .orElseThrow(() -> new IllegalArgumentException("Attempted to use property that was not registered: " + assignment.property().getId(catalog)));

        return new RecordTypeProperty<>(property, assignment.defaultValue());
    }

    @Override
    public Optional<RecordType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.recordTypeRepo.findById(id);
    }
}
