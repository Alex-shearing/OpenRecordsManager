package com.openrecordsmanager.plugin.registry.mapper;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.PropertyAssignment;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.RecordTypeTemplate;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.type.RecordType;
import com.openrecordsmanager.template.ManagedObjectType;
import com.openrecordsmanager.template.ManagedObjectTypeProperty;

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
        Set<ManagedObjectTypeProperty<?>> properties = component.properties()
                .stream()
                .map(def -> createTypeProperty(def, catalog, repository))
                .collect(Collectors.<ManagedObjectTypeProperty<?>>toSet());

        ManagedObjectType baseType = null;
        if (component.baseType() != null) {
            baseType = repository.recordTypeRepo.findById(component.baseType().id())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Attempted to use base record type that was not registered: " + component.baseType()
                    ));
        }

        RecordType type = new RecordType(
                id,
                component.abstractType(),
                baseType,
                component.allowedContentTypes(),
                expressions.buildExpression(component.securityFilter()),
                component.securityFilterUsage(),
                properties
        );
        repository.recordTypeRepo.saveAndFlush(type);
    }

    @SuppressWarnings("unchecked")
    private static <T> ManagedObjectTypeProperty<T> createTypeProperty(
            PropertyAssignment<T> assignment,
            ComponentCatalog catalog,
            DataRepository repository
    ) {
        ResourceIdentifier id = assignment.property().id();
        ObjectProperty<T> property = (ObjectProperty<T>) catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(id, repository)
                .orElseThrow(() -> new IllegalArgumentException("Attempted to use property that was not registered: " + id));

        return new ManagedObjectTypeProperty<>(
                property, PropertyType.toTree(assignment.defaultValue())
        );
    }

    @Override
    public Optional<RecordType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.recordTypeRepo.findById(id);
    }
}
