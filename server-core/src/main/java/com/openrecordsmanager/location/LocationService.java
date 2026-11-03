package com.openrecordsmanager.location;

import com.openrecordsmanager.action.ActionContextImpl;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.action.ActionContext;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.location.LocationActionType;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.LocationSearchResponse;
import com.openrecordsmanager.location.dto.NewLocationRequest;
import com.openrecordsmanager.location.dto.UpdateLocationRequest;
import com.openrecordsmanager.location.group.Group;
import com.openrecordsmanager.location.group.GroupService;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.location.user.UserService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.dto.ActionResponse;
import com.openrecordsmanager.rest.exception.ActionNotAvailableException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.schema.JsonSchemaValidator;
import com.openrecordsmanager.search.dto.ObjectSearchRequest;
import com.openrecordsmanager.search.sql.BuiltinColumnResolver;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final LocationSearchSupport searchSupport;
    private final ObjectSearchSchema searchSchema;
    private final UserService userService;
    private final GroupService groupService;

    public LocationService(
            DataRepository repository,
            AuditService auditService,
            AuditPolicyService auditPolicyService,
            ConfigService config,
            ComponentCatalog catalog,
            LocationSearchSupport searchSupport,
            BuiltinColumnResolver columnResolver,
            UserService userService,
            GroupService groupService
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.auditPolicyService = auditPolicyService;
        this.config = config;
        this.catalog = catalog;
        this.searchSupport = searchSupport;
        this.searchSchema = ObjectSearchSchema.of(
                SearchFieldTarget.LOCATION,
                Location.class,
                Location.BUILTIN_PROPERTY_BINDINGS,
                columnResolver
        );
        this.userService = userService;
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public LocationSearchResponse search(User actor, @Nullable ManagedObjectKind kind, ObjectSearchRequest request) {
        return switch (kind) {
            case USER -> this.userService.search(actor, request);
            case GROUP -> this.groupService.search(actor, request);
            case null, default -> this.searchSupport.search(
                    this.searchSchema,
                    actor,
                    request,
                    ids -> this.repository.locationRepo.findAllById(ids).stream()
                            .collect(Collectors.toMap(Location::getId, Function.identity()))
            );
        };
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

        ManagedObjectKind kind = type.getKindRequireInstantiable();
        return switch (kind) {
            case USER -> this.userService.create(type, input);
            case GROUP -> this.groupService.create(type, input);
            default -> throw ApiException.validationFailed("type", "invalid_location_type_kind", kind.key());
        };
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.LOCATION)
    public LocationResponse update(User actor, UUID id, UpdateLocationRequest input) {
        Location location = this.repository.locationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("location", id));

        return switch (location.getKind()) {
            case USER -> this.userService.update(actor, (User) location, input);
            case GROUP -> this.groupService.update((Group) location, input);
            default -> throw ApiException.validationFailed(
                    "id", "invalid_location_kind", location.getKind().key()
            );
        };
    }

    @Transactional(readOnly = true)
    public Set<ActionResponse> listActions(User actor, UUID id) {
        Location target = this.repository.locationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("location", id));

        ActionContext context = this.actionContext(actor, target);

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

        ActionContext context = this.actionContext(actor, target);

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

    private ActionContext actionContext(User actor, Location target) {
        return new ActionContextImpl(
                this.repository,
                this.catalog,
                this.config,
                this.auditService,
                actor,
                target,
                AuditEntityType.LOCATION,
                managed -> this.repository.locationRepo.saveAndFlush((Location) managed)
        );
    }

    private static <I extends Record> void parseAndExecute(
            LocationActionType<I> action,
            ActionContext context,
            Map<String, ?> inputs
    ) {
        action.execute(context, JsonSchemaValidator.toRecord(action.getInputClass(), inputs));
    }
}
