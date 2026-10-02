package com.openrecordsmanager.location.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.relationship.dto.LocationRelationshipTypeResponse;
import com.openrecordsmanager.location.relationship.dto.NewLocationRelationshipTypeRequest;
import com.openrecordsmanager.location.relationship.dto.UpdateLocationRelationshipTypeRequest;
import com.openrecordsmanager.plugin.exception.BuiltinResourceImmutableException;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LocationRelationshipTypeService {

    private final DataRepository repository;
    private final AuditService auditService;

    public LocationRelationshipTypeService(DataRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Set<LocationRelationshipTypeResponse> getAll() {
        Set<LocationRelationshipTypeResponse> results = this.repository.locationRelationshipTypeRepo.findAll().stream()
                .map(LocationRelationshipTypeResponse::of)
                .collect(Collectors.toSet());
        this.auditService.recordCollectionRead(AuditEntityType.LOCATION_RELATIONSHIP_TYPE, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public LocationRelationshipTypeResponse get(ResourceIdentifier id) {
        LocationRelationshipType type = this.repository.locationRelationshipTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LOCATION_RELATIONSHIP_TYPE, id));

        this.auditService.addReadEvent(AuditEntityType.LOCATION_RELATIONSHIP_TYPE, id);
        return LocationRelationshipTypeResponse.of(type);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.LOCATION_RELATIONSHIP_TYPE)
    public LocationRelationshipTypeResponse create(NewLocationRelationshipTypeRequest input) {
        if (this.repository.locationRelationshipTypeRepo.existsById(input.id())) {
            throw new ResourceAlreadyExistsException(ComponentTypes.LOCATION_RELATIONSHIP_TYPE, input.id());
        }
        if (input.id().isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }

        LocationRelationshipType type = new LocationRelationshipType(
                input.id(),
                input.sourceKind(),
                input.targetKind(),
                input.uniquePerSource()
        );
        this.repository.locationRelationshipTypeRepo.saveAndFlush(type);

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.LOCATION_RELATIONSHIP_TYPE, type.getId());
        return LocationRelationshipTypeResponse.of(type);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.LOCATION_RELATIONSHIP_TYPE)
    public LocationRelationshipTypeResponse update(ResourceIdentifier id, UpdateLocationRelationshipTypeRequest input) {
        LocationRelationshipType type = this.repository.locationRelationshipTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LOCATION_RELATIONSHIP_TYPE, id));

        if (id.isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }

        if (input.sourceKind() != null) {
            type.setSourceKind(input.sourceKind());
        }
        if (input.targetKind() != null) {
            type.setTargetKind(input.targetKind());
        }
        if (input.uniquePerSource() != null) {
            type.setUniquePerSource(input.uniquePerSource());
        }

        this.repository.locationRelationshipTypeRepo.saveAndFlush(type);
        this.auditService.addEvent(AuditOperation.UPDATE, AuditEntityType.LOCATION_RELATIONSHIP_TYPE, id);
        return LocationRelationshipTypeResponse.of(type);
    }
}
