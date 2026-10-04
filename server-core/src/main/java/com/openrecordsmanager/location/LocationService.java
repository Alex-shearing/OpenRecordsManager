package com.openrecordsmanager.location;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.location.LocationActionType;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditPropertyChange;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.LocationSearchRequest;
import com.openrecordsmanager.location.dto.LocationSearchResponse;
import com.openrecordsmanager.location.dto.NewLocationRequest;
import com.openrecordsmanager.location.dto.UpdateLocationRequest;
import com.openrecordsmanager.location.group.Group;
import com.openrecordsmanager.location.relationship.LocationRelationship;
import com.openrecordsmanager.location.relationship.LocationRelationshipType;
import com.openrecordsmanager.location.relationship.RelationshipDirection;
import com.openrecordsmanager.location.relationship.dto.LocationRelationshipResponse;
import com.openrecordsmanager.location.relationship.dto.NewLocationRelationshipRequest;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.location.user.UserService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectPropertyApplier;
import com.openrecordsmanager.rest.dto.ActionResponse;
import com.openrecordsmanager.rest.exception.ActionNotAvailableException;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.schema.JsonSchemaValidator;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final DataRepository repository;
    private final AuditService auditService;
    private final AuditPolicyService auditPolicyService;
    private final ConfigService config;
    private final ComponentCatalog catalog;
    private final ObjectSearchExecutor searchExecutor;
    private final ObjectPropertyApplier propertyApplier;
    private final ObjectSearchSchema locationSearchSchema;
    private final ObjectSearchSchema userSearchSchema;
    private final ObjectSearchSchema groupSearchSchema;
    private final UserService userService;

    public LocationService(
            DataRepository repository,
            AuditService auditService,
            AuditPolicyService auditPolicyService,
            ConfigService config,
            ComponentCatalog catalog,
            ObjectSearchExecutor searchExecutor,
            BuiltinColumnResolver columnResolver,
            ObjectPropertyApplier propertyApplier,
            UserService userService
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.auditPolicyService = auditPolicyService;
        this.config = config;
        this.catalog = catalog;
        this.searchExecutor = searchExecutor;
        this.propertyApplier = propertyApplier;
        this.locationSearchSchema = ObjectSearchSchema.of(
                SearchFieldTarget.LOCATION,
                Location.class,
                Location.BUILTIN_PROPERTY_BINDINGS,
                columnResolver
        );
        this.userSearchSchema = ObjectSearchSchema.of(
                SearchFieldTarget.USER,
                User.class,
                User.BUILTIN_PROPERTY_BINDINGS,
                columnResolver
        );
        this.groupSearchSchema = ObjectSearchSchema.of(
                SearchFieldTarget.LOCATION,
                Group.class,
                Group.BUILTIN_PROPERTY_BINDINGS,
                columnResolver
        );
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public LocationSearchResponse search(User actor, LocationSearchRequest request) {
        LocationKind kind = request.kind();
        if (kind == LocationKind.USER) {
            return this.searchWith(
                    this.userSearchSchema,
                    actor,
                    request,
                    ids -> this.repository.userRepo.findAllById(ids).stream()
                            .collect(Collectors.toMap(User::getId, Function.identity()))
            );
        }
        if (kind == LocationKind.GROUP) {
            return this.searchWith(
                    this.groupSearchSchema,
                    actor,
                    request,
                    ids -> this.repository.groupRepo.findAllById(ids).stream()
                            .collect(Collectors.toMap(Group::getId, Function.identity()))
            );
        }
        return this.searchWith(
                this.locationSearchSchema,
                actor,
                request,
                ids -> this.repository.locationRepo.findAllById(ids).stream()
                        .collect(Collectors.toMap(Location::getId, Function.identity()))
        );
    }

    private LocationSearchResponse searchWith(
            ObjectSearchSchema schema,
            User actor,
            LocationSearchRequest request,
            Function<List<UUID>, Map<UUID, ? extends Location>> loader
    ) {
        int pageLimit = request.limitOrDefault();
        List<UUID> ids = this.searchExecutor.searchIds(
                schema,
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

        Map<UUID, ? extends Location> loaded = loader.apply(ids);

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
    public LocationResponse me(User user) {
        return this.get(user.getId());
    }

    @Transactional(readOnly = true)
    public LocationResponse get(UUID id) {
        Location location = this.repository.locationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("location", id));

        this.auditService.addReadEvent(AuditEntityType.LOCATION, id);
        return LocationResponse.of(location);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.LOCATION)
    public LocationResponse create(NewLocationRequest input) {
        LocationType type = this.repository.locationTypeRepo.findById(input.type())
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LOCATION_TYPE, input.type()));

        return switch (type.getKind()) {
            case USER -> this.userService.create(type, input);
            case GROUP -> this.createGroup(type, input);
            case ANY -> throw ApiException.validationFailed("type", "invalid_location_type_kind", type.getKind().key());
        };
    }

    private LocationResponse createGroup(LocationType type, NewLocationRequest input) {
        String name = input.name();
        if (name == null || name.isBlank()) {
            throw ApiException.validationFailed("name", "required");
        }
        if (this.repository.locationRepo.findByName(name).isPresent()) {
            throw new ResourceAlreadyExistsException("location", name);
        }

        List<AuditPropertyChange> changes = new ArrayList<>();
        changes.add(AuditPropertyChange.newProperty("type", input.type()));
        changes.add(AuditPropertyChange.newProperty("name", name));

        Group group = new Group(name, type);
        this.propertyApplier.applyOnCreate(group, input.properties(), true, changes);

        this.repository.groupRepo.saveAndFlush(group);

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.LOCATION,
                group.getId().toString(),
                changes,
                null,
                null
        );

        return LocationResponse.of(group);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.LOCATION)
    public LocationResponse update(User actor, UUID id, UpdateLocationRequest input) {
        Location location = this.repository.locationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("location", id));

        return switch (location.getKind()) {
            case USER -> this.userService.update(actor, (User) location, input);
            case GROUP -> this.updateGroup((Group) location, input);
            case ANY -> throw ApiException.validationFailed("id", "invalid_location_kind", location.getKind().key());
        };
    }

    private LocationResponse updateGroup(Group group, UpdateLocationRequest input) {
        List<AuditPropertyChange> changes = new ArrayList<>();

        if (input.name() != null && !input.name().equals(group.getName())) {
            if (this.repository.locationRepo.findByName(input.name()).isPresent()) {
                throw new ResourceAlreadyExistsException("location", input.name());
            }
            String oldName = group.getName();
            group.setName(input.name());
            changes.add(AuditPropertyChange.of("name", oldName, input.name()));
        }

        if (input.properties() != null) {
            this.propertyApplier.applyOnUpdate(group, input.properties(), true, changes);
        }

        this.repository.groupRepo.saveAndFlush(group);

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.LOCATION,
                group.getId().toString(),
                changes.isEmpty() ? null : changes,
                null,
                null
        );

        return LocationResponse.of(group);
    }

    @Transactional(readOnly = true)
    public Set<ActionResponse> listActions(User actor, UUID id) {
        Location target = this.repository.locationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("location", id));

        LocationActionContextImpl context = new LocationActionContextImpl(
                this.repository,
                this.catalog,
                this.config,
                this.auditService,
                actor,
                target
        );

        Set<ActionResponse> actions = this.catalog.getRegistry(ComponentTypes.LOCATION_ACTION).stream()
                .filter(action -> action.isAvailable(context))
                .map(action -> ActionResponse.ofLocation(this.catalog, action, this.auditPolicyService))
                .collect(Collectors.toSet());

        this.auditService.addReadEvent(AuditEntityType.LOCATION, id);
        return actions;
    }

    @Transactional
    public void executeAction(User actor, UUID id, ResourceIdentifier actionId, Map<String, ?> inputs) {
        LocationActionType<?> action = this.catalog.getRegistry(ComponentTypes.LOCATION_ACTION).get(actionId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LOCATION_ACTION, actionId));

        Location target = this.repository.locationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("location", id));

        LocationActionContextImpl context = new LocationActionContextImpl(
                this.repository,
                this.catalog,
                this.config,
                this.auditService,
                actor,
                target
        );

        if (!action.isAvailable(context)) {
            throw new ActionNotAvailableException(actionId, "location", id);
        }

        this.auditPolicyService.validateCommentRequired(AuditEntityType.LOCATION, AuditOperation.ACTION);

        parseAndExecute(action, context, inputs);

        this.auditService.addActionRanEvent(
                actionId,
                AuditEntityType.LOCATION,
                id,
                Map.of("inputs", inputs.keySet())
        );
    }

    private static <I extends Record> void parseAndExecute(
            LocationActionType<I> action,
            LocationActionContextImpl context,
            Map<String, ?> inputs
    ) {
        action.execute(context, JsonSchemaValidator.toRecord(action.getInputClass(), inputs));
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
