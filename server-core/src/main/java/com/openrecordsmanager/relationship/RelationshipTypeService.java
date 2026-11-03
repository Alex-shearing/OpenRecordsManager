package com.openrecordsmanager.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.exception.BuiltinResourceImmutableException;
import com.openrecordsmanager.relationship.dto.NewRelationshipTypeRequest;
import com.openrecordsmanager.relationship.dto.RelationshipTypeResponse;
import com.openrecordsmanager.relationship.dto.UpdateRelationshipTypeRequest;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.template.ManagedObjectType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RelationshipTypeService {

    private final DataRepository repository;
    private final AuditService auditService;

    public RelationshipTypeService(DataRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Set<RelationshipTypeResponse> getAll() {
        Set<RelationshipTypeResponse> results = this.repository.relationshipTypeRepo.findAll().stream()
                .map(RelationshipTypeResponse::of)
                .collect(Collectors.toSet());
        this.auditService.recordCollectionRead(AuditEntityType.RELATIONSHIP_TYPE, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public RelationshipTypeResponse get(ResourceIdentifier id) {
        RelationshipType type = this.require(id);
        this.auditService.addReadEvent(AuditEntityType.RELATIONSHIP_TYPE, id);
        return RelationshipTypeResponse.of(type);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.RELATIONSHIP_TYPE)
    public RelationshipTypeResponse create(NewRelationshipTypeRequest input) {
        if (this.repository.relationshipTypeRepo.existsById(input.id())) {
            throw new ResourceAlreadyExistsException(ComponentTypes.RELATIONSHIP_TYPE, input.id());
        }
        if (input.id().isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }
        ManagedObjectType sourceType = this.requireManagedObjectType(input.sourceTypeId());
        ManagedObjectType targetType = this.requireManagedObjectType(input.targetTypeId());

        RelationshipType type = new RelationshipType(
                input.id(),
                sourceType,
                targetType,
                input.uniquePerSource()
        );
        this.repository.relationshipTypeRepo.saveAndFlush(type);

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.RELATIONSHIP_TYPE, type.getId());
        return RelationshipTypeResponse.of(type);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.RELATIONSHIP_TYPE)
    public RelationshipTypeResponse update(ResourceIdentifier id, UpdateRelationshipTypeRequest input) {
        RelationshipType type = this.require(id);

        if (id.isBuiltin()) {
            throw new BuiltinResourceImmutableException();
        }

        if (input.sourceTypeId() != null) {
            type.setSourceType(this.requireManagedObjectType(input.sourceTypeId()));
        }
        if (input.targetTypeId() != null) {
            type.setTargetType(this.requireManagedObjectType(input.targetTypeId()));
        }
        if (input.uniquePerSource() != null) {
            type.setUniquePerSource(input.uniquePerSource());
        }

        this.repository.relationshipTypeRepo.saveAndFlush(type);
        this.auditService.addEvent(AuditOperation.UPDATE, AuditEntityType.RELATIONSHIP_TYPE, id);
        return RelationshipTypeResponse.of(type);
    }

    private RelationshipType require(ResourceIdentifier id) {
        return this.repository.relationshipTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RELATIONSHIP_TYPE, id));
    }

    private ManagedObjectType requireManagedObjectType(ResourceIdentifier typeId) {
        return this.repository.managedObjectTypeRepo.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("managed_object_type", typeId.toString()));
    }
}
