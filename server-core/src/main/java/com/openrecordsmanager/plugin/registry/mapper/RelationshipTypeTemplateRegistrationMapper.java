package com.openrecordsmanager.plugin.registry.mapper;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.template.relationship.RelationshipTypeTemplate;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.relationship.RelationshipType;
import com.openrecordsmanager.template.ManagedObjectType;

import java.util.Optional;

public class RelationshipTypeTemplateRegistrationMapper
        extends TemplateRegistrationMapper<RelationshipTypeTemplate, RelationshipType> {

    @Override
    public ComponentType<RelationshipTypeTemplate> componentType() {
        return ComponentTypes.RELATIONSHIP_TYPE;
    }

    @Override
    protected void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            AuditService auditService,
            ResourceIdentifier id,
            RelationshipTypeTemplate component
    ) {
        ManagedObjectType sourceType = requireManagedObjectType(component.sourceType(), catalog, repository);
        ManagedObjectType targetType = requireManagedObjectType(component.targetType(), catalog, repository);
        RelationshipType type = new RelationshipType(
                id,
                sourceType,
                targetType,
                component.uniquePerSource()
        );
        repository.relationshipTypeRepo.saveAndFlush(type);
    }

    private static ManagedObjectType requireManagedObjectType(
            ComponentReference<? extends TemplateComponent> reference,
            ComponentCatalog catalog,
            DataRepository repository
    ) {
        Object registered = catalog
                .getTemplateRegistry(ComponentCatalog.mapperFromComponent(reference.type()))
                .getRegistered(reference.id(), repository)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Attempted to use type that was not registered: " + reference
                ));
        if (!(registered instanceof ManagedObjectType managedObjectType)) {
            throw new IllegalArgumentException(
                    "Relationship endpoint must be a managed object type: " + reference
            );
        }
        return managedObjectType;
    }

    @Override
    public Optional<RelationshipType> getRegistered(ResourceIdentifier id, DataRepository repo) {
        return repo.relationshipTypeRepo.findById(id);
    }
}
