package com.openrecordsmanager.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.ManagedObject;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.record.RecordService;
import com.openrecordsmanager.relationship.dto.NewRelationshipRequest;
import com.openrecordsmanager.relationship.dto.RelationshipResponse;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.template.ManagedObjectType;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RelationshipService {

    private final DataRepository repository;
    private final ExpressionsService expressions;
    private final AuditService auditService;
    private final RecordService recordService;

    public RelationshipService(
            DataRepository repository,
            ExpressionsService expressions,
            AuditService auditService,
            RecordService recordService
    ) {
        this.repository = repository;
        this.expressions = expressions;
        this.auditService = auditService;
        this.recordService = recordService;
    }

    @Transactional(readOnly = true)
    public List<RelationshipResponse> list(
            User actor,
            UUID holderId,
            RelationshipDirection direction,
            @Nullable ResourceIdentifier typeId
    ) {
        ManagedObject<?> entity = this.requireManagedObject(actor, holderId);

        List<Relationship> relationships = direction == RelationshipDirection.INCOMING
                ? this.repository.relationshipRepo.findActiveIncoming(holderId, typeId)
                : this.repository.relationshipRepo.findActiveOutgoing(holderId, typeId);

        this.auditService.addReadEvent(auditEntityType(entity), holderId);
        return relationships.stream()
                .filter(relationship -> {
                    ManagedObject<?> peer = direction == RelationshipDirection.INCOMING
                            ? relationship.getSource()
                            : relationship.getTarget();
                    return this.isPeerVisible(actor, peer);
                })
                .map(RelationshipResponse::of)
                .toList();
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.RELATIONSHIP)
    public RelationshipResponse create(User actor, UUID sourceId, NewRelationshipRequest input) {
        ManagedObject<?> source = this.requireManagedObject(actor, sourceId);
        ManagedObject<?> target = this.requireManagedObject(actor, input.targetId());
        RelationshipType type = this.repository.relationshipTypeRepo.findById(input.typeId())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.RELATIONSHIP_TYPE, input.typeId()));

        this.validateNewEdge(
                type.getSourceType(),
                source,
                type.getTargetType(),
                target,
                this.repository.relationshipRepo
                        .findActiveEdge(sourceId, input.targetId(), input.typeId())
                        .isPresent(),
                input.typeId(),
                type.isUniquePerSource(),
                !this.repository.relationshipRepo
                        .findActiveBySourceAndType(sourceId, input.typeId())
                        .isEmpty()
        );

        Relationship relationship = new Relationship(source, target, type);
        if (input.activeTo() != null) {
            relationship.end(input.activeTo());
        }

        this.repository.relationshipRepo.saveAndFlush(relationship);

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.RELATIONSHIP,
                relationship.getId().toString()
        );

        return RelationshipResponse.of(relationship);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.RELATIONSHIP)
    public void end(User actor, UUID entityId, UUID relationshipId) {
        this.requireManagedObject(actor, entityId);
        Relationship relationship = this.repository.relationshipRepo.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("relationship", relationshipId));

        if (!relationship.getSource().getId().equals(entityId)
                && !relationship.getTarget().getId().equals(entityId)) {
            throw new ResourceNotFoundException("relationship", relationshipId);
        }

        this.requireManagedObject(actor, relationship.getSource().getId());
        this.requireManagedObject(actor, relationship.getTarget().getId());

        if (relationship.isActive()) {
            relationship.end(Instant.now());
            this.repository.relationshipRepo.saveAndFlush(relationship);
        }

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.RELATIONSHIP,
                relationshipId.toString()
        );
    }

    private void validateNewEdge(
            ManagedObjectType expectedSourceType,
            ManagedObject<?> source,
            ManagedObjectType expectedTargetType,
            ManagedObject<?> target,
            boolean alreadyPresent,
            ResourceIdentifier typeId,
            boolean uniquePerSource,
            boolean hasExistingForSource
    ) {
        this.assertEndpointKind(expectedSourceType, source);
        this.assertEndpointKind(expectedTargetType, target);
        this.assertTypeAssignable(expectedSourceType, source.getType(), "source");
        this.assertTypeAssignable(expectedTargetType, target.getType(), "target");

        if (source.getId().equals(target.getId())) {
            throw ApiException.validationFailed("targetId", "relationship_self_not_allowed");
        }
        if (alreadyPresent) {
            throw new ResourceAlreadyExistsException(
                    "relationship",
                    typeId + ":" + source.getId() + "->" + target.getId()
            );
        }
        if (uniquePerSource && hasExistingForSource) {
            throw ApiException.validationFailed(
                    "typeId",
                    "relationship_unique_per_source",
                    typeId.toString()
            );
        }
    }

    private void assertTypeAssignable(
            ManagedObjectType constraint,
            ManagedObjectType actual,
            String role
    ) {
        if (!actual.isAssignableTo(constraint)) {
            throw ApiException.validationFailed("typeId", "relationship_type_mismatch", role);
        }
    }

    private void assertEndpointKind(ManagedObjectType expectedType, ManagedObject<?> actual) {
        ManagedObjectKind expected = expectedType.getKind();
        ManagedObjectKind actualKind = actual.getKind();
        if (!endpointKindMatches(expected, actualKind)) {
            throw ApiException.validationFailed(
                    "typeId",
                    "relationship_endpoint_kind_mismatch",
                    String.valueOf(expected),
                    actualKind.name()
            );
        }
    }

    /**
     * {@code null} expected kind means abstract any-location (USER or GROUP).
     */
    private static boolean endpointKindMatches(
            @Nullable ManagedObjectKind expected,
            ManagedObjectKind actual
    ) {
        if (expected == null) {
            return actual.isLocation();
        }
        return expected == actual;
    }

    private ManagedObject<?> requireManagedObject(User actor, UUID id) {
        ManagedObject<?> entity = this.repository.managedObjectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("object property holder", id));
        if (entity instanceof Record) {
            return this.recordService.requireVisibleRecord(actor, id);
        }
        return entity;
    }

    private boolean isPeerVisible(User actor, ManagedObject<?> peer) {
        if (peer instanceof Record record) {
            return record.securityFilter(this.expressions, actor).canSeeMetadata();
        }
        return true;
    }

    private static AuditEntityType auditEntityType(ManagedObject<?> entity) {
        return entity.getKind().isLocation()
                ? AuditEntityType.LOCATION
                : AuditEntityType.RECORD;
    }
}
