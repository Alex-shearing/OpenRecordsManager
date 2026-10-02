package com.openrecordsmanager.location;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.LocationSearchRequest;
import com.openrecordsmanager.location.dto.LocationSearchResponse;
import com.openrecordsmanager.location.relationship.LocationRelationship;
import com.openrecordsmanager.location.relationship.LocationRelationshipType;
import com.openrecordsmanager.location.relationship.RelationshipDirection;
import com.openrecordsmanager.location.relationship.dto.LocationRelationshipResponse;
import com.openrecordsmanager.location.relationship.dto.NewLocationRelationshipRequest;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.search.ObjectSearchExecutor;
import com.openrecordsmanager.search.sql.BuiltinColumnResolver;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final DataRepository repository;
    private final AuditService auditService;
    private final ObjectSearchExecutor searchExecutor;
    private final ObjectSearchSchema searchSchema;

    public LocationService(
            DataRepository repository,
            AuditService auditService,
            ObjectSearchExecutor searchExecutor,
            BuiltinColumnResolver columnResolver
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.searchExecutor = searchExecutor;
        this.searchSchema = ObjectSearchSchema.of(
                SearchFieldTarget.LOCATION,
                Location.class,
                Location.BUILTIN_PROPERTY_BINDINGS,
                columnResolver
        );
    }

    @Transactional(readOnly = true)
    public LocationSearchResponse search(User actor, LocationSearchRequest request) {
        int pageLimit = request.limitOrDefault();
        List<UUID> ids = this.searchExecutor.searchIds(
                this.searchSchema,
                actor,
                request.q(),
                request.filters(),
                request.matchOrDefault(),
                null,
                request.cursor(),
                pageLimit + 1
        );

        boolean hasMore = ids.size() > pageLimit;
        if (hasMore) {
            ids = ids.subList(0, pageLimit);
        }

        Map<UUID, Location> loaded = this.repository.locationRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(Location::getId, l -> l));

        List<LocationResponse> items = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            Location location = loaded.get(id);
            if (location != null) {
                items.add(LocationResponse.of(location));
            }
        }

        UUID nextCursor = hasMore && !items.isEmpty() ? items.getLast().id() : null;

        this.auditService.recordSearchRead(
                AuditEntityType.LOCATION,
                AuditService.COLLECTION_TARGET_ID,
                this.searchExecutor.summarize(request.q(), request.filters()),
                items.size()
        );

        return new LocationSearchResponse(List.copyOf(items), nextCursor);
    }

    @Transactional(readOnly = true)
    public LocationResponse get(UUID id) {
        Location location = this.repository.locationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("location", id));

        this.auditService.addReadEvent(AuditEntityType.LOCATION, id);
        return LocationResponse.of(location);
    }

    @Transactional(readOnly = true)
    public List<LocationRelationshipResponse> listRelationships(
            UUID locationId,
            RelationshipDirection direction,
            @Nullable ResourceIdentifier typeId
    ) {
        if (!this.repository.locationRepo.existsById(locationId)) {
            throw new ResourceNotFoundException("location", locationId);
        }

        List<LocationRelationship> relationships = direction == RelationshipDirection.INCOMING
                ? this.repository.locationRelationshipRepo.findActiveIncoming(locationId, typeId)
                : this.repository.locationRelationshipRepo.findActiveOutgoing(locationId, typeId);

        this.auditService.addReadEvent(AuditEntityType.LOCATION, locationId);
        return relationships.stream().map(LocationRelationshipResponse::of).toList();
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.LOCATION_RELATIONSHIP)
    public LocationRelationshipResponse createRelationship(UUID sourceId, NewLocationRelationshipRequest input) {
        Location source = this.repository.locationRepo.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("location", sourceId));
        Location target = this.repository.locationRepo.findById(input.targetId())
                .orElseThrow(() -> new ResourceNotFoundException("location", input.targetId()));
        LocationRelationshipType type = this.repository.locationRelationshipTypeRepo.findById(input.typeId())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LOCATION_RELATIONSHIP_TYPE, input.typeId()));

        if (!type.getSourceKind().matches(source.getKind())) {
            throw ApiException.validationFailed("typeId", "relationship_kind_mismatch", "source");
        }
        if (!type.getTargetKind().matches(target.getKind())) {
            throw ApiException.validationFailed("typeId", "relationship_kind_mismatch", "target");
        }
        if (source.getId().equals(target.getId())) {
            throw ApiException.validationFailed("targetId", "relationship_self_not_allowed");
        }

        if (this.repository.locationRelationshipRepo.findActiveEdge(sourceId, input.targetId(), input.typeId()).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "location relationship",
                    input.typeId() + ":" + sourceId + "->" + input.targetId()
            );
        }

        if (type.isUniquePerSource()) {
            List<LocationRelationship> existing = this.repository.locationRelationshipRepo
                    .findActiveBySourceAndType(sourceId, input.typeId());
            if (!existing.isEmpty()) {
                throw ApiException.validationFailed("typeId", "relationship_unique_per_source", input.typeId().toString());
            }
        }

        LocationRelationship relationship = new LocationRelationship(source, target, type);
        if (input.activeTo() != null) {
            relationship.end(input.activeTo());
        }

        this.repository.locationRelationshipRepo.saveAndFlush(relationship);

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.LOCATION_RELATIONSHIP,
                relationship.getId().toString()
        );

        return LocationRelationshipResponse.of(relationship);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.LOCATION_RELATIONSHIP)
    public void endRelationship(UUID locationId, UUID relationshipId) {
        LocationRelationship relationship = this.repository.locationRelationshipRepo.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("location relationship", relationshipId));

        if (!relationship.getSource().getId().equals(locationId)
                && !relationship.getTarget().getId().equals(locationId)) {
            throw new ResourceNotFoundException("location relationship", relationshipId);
        }

        if (relationship.isActive()) {
            relationship.end(Instant.now());
            this.repository.locationRelationshipRepo.saveAndFlush(relationship);
        }

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.LOCATION_RELATIONSHIP,
                relationshipId.toString()
        );
    }
}
