package com.openrecordsmanager.plugin.registry.mapper;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.location.LocationTypeTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.PropertyAssignment;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.type.LocationTypeProperty;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectProperty;

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
        Set<LocationTypeProperty<?>> properties = component.properties()
                .stream()
                .map(def -> createLocationTypeProperty(def, catalog, repository))
                .collect(Collectors.<LocationTypeProperty<?>>toSet());

        LocationType type = new LocationType(id, component.kind(), properties);
        repository.locationTypeRepo.saveAndFlush(type);
    }

    @SuppressWarnings("unchecked")
    private static <T> LocationTypeProperty<T> createLocationTypeProperty(
            PropertyAssignment<T> assignment,
            ComponentCatalog catalog,
            DataRepository repository
    ) {
        ResourceIdentifier id = assignment.property().getId(catalog)
                .orElseThrow(() -> new IllegalArgumentException("id " + assignment.property() + " is not found"));
        ObjectProperty<T> property = (ObjectProperty<T>) catalog
                .getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(id, repository)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Attempted to use property that was not registered: " + assignment.property().getId(catalog)
                ));

        return new LocationTypeProperty<>(property, PropertyType.toTree(assignment.defaultValue()));
    }

    @Override
    public Optional<LocationType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.locationTypeRepo.findById(id);
    }
}