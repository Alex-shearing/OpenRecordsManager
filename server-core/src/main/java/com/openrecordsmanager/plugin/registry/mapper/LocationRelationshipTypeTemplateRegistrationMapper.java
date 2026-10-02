package com.openrecordsmanager.plugin.registry.mapper;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.location.LocationRelationshipTypeTemplate;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.relationship.LocationRelationshipType;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;

import java.util.Optional;

public class LocationRelationshipTypeTemplateRegistrationMapper
        extends TemplateRegistrationMapper<LocationRelationshipTypeTemplate, LocationRelationshipType> {

    @Override
    public ComponentType<LocationRelationshipTypeTemplate> componentType() {
        return ComponentTypes.LOCATION_RELATIONSHIP_TYPE;
    }

    @Override
    protected void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            ResourceIdentifier id,
            LocationRelationshipTypeTemplate component
    ) {
        LocationRelationshipType type = new LocationRelationshipType(
                id,
                component.sourceKind(),
                component.targetKind(),
                component.uniquePerSource()
        );
        repository.locationRelationshipTypeRepo.saveAndFlush(type);
    }

    @Override
    public Optional<LocationRelationshipType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.locationRelationshipTypeRepo.findById(id);
    }
}
