package com.openrecordsmanager.plugin.registry.mapper;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.PropertyAssignment;
import com.openrecordsmanager.api.template.location.LocationTypeTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.template.ManagedObjectType;
import com.openrecordsmanager.template.ManagedObjectTypeProperty;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LocationTypeTemplateRegistrationMapper
        extends TemplateRegistrationMapper<LocationTypeTemplate, LocationType> {

    @Override
    public ComponentType<LocationTypeTemplate> componentType() {
        return ComponentTypes.LOCATION_TYPE;
    }

    @Override
    protected void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            ResourceIdentifier id,
            LocationTypeTemplate component
    ) {
        Set<ManagedObjectTypeProperty<?>> properties = component.properties()
                .stream()
                .map(def -> createTypeProperty(def, catalog, repository))
                .collect(Collectors.<ManagedObjectTypeProperty<?>>toSet());

        ManagedObjectType baseType = null;
        if (component.baseType() != null) {
            baseType = repository.locationTypeRepo.findById(component.baseType().id())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Attempted to use base location type that was not registered: " + component.baseType()
                    ));
        }

        LocationType type = new LocationType(
                id,
                component.kind(),
                component.abstractType(),
                baseType,
                properties
        );
        repository.locationTypeRepo.saveAndFlush(type);
    }

    @SuppressWarnings("unchecked")
    private static <T> ManagedObjectTypeProperty<T> createTypeProperty(
            PropertyAssignment<T> assignment,
            ComponentCatalog catalog,
            DataRepository repository
    ) {
        ResourceIdentifier id = assignment.property().id();

        ObjectProperty<T> property = (ObjectProperty<T>) catalog
                .getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(id, repository)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Attempted to use property that was not registered: " + id
                ));

        return new ManagedObjectTypeProperty<>(property, PropertyType.toTree(assignment.defaultValue()));
    }

    @Override
    public Optional<LocationType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.locationTypeRepo.findById(id);
    }
}