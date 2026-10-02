package com.openrecordsmanager.location.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.search.SearchFieldTarget;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.api.user.UserActionType;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditPropertyChange;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectPropertyApplier;
import com.openrecordsmanager.rest.dto.ActionResponse;
import com.openrecordsmanager.rest.exception.ActionNotAvailableException;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceInUseException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.schema.JsonSchemaValidator;
import com.openrecordsmanager.search.ObjectSearchExecutor;
import com.openrecordsmanager.search.sql.BuiltinColumnResolver;
import com.openrecordsmanager.search.sql.ObjectSearchSchema;
import com.openrecordsmanager.location.user.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final DataRepository repository;
    private final ConfigService config;
    private final ComponentCatalog catalog;
    private final AuditService auditService;
    private final AuditPolicyService auditPolicyService;
    private final ObjectPropertyApplier propertyApplier;
    private final ObjectSearchExecutor searchExecutor;
    private final ObjectSearchSchema searchSchema;

    public UserService(
            DataRepository repository,
            ConfigService config,
            ComponentCatalog catalog,
            AuditService auditService,
            AuditPolicyService auditPolicyService,
            ObjectPropertyApplier propertyApplier,
            ObjectSearchExecutor searchExecutor,
            BuiltinColumnResolver columnResolver
    ) {
        this.repository = repository;
        this.config = config;
        this.catalog = catalog;
        this.auditService = auditService;
        this.auditPolicyService = auditPolicyService;
        this.propertyApplier = propertyApplier;
        this.searchExecutor = searchExecutor;
        this.searchSchema = ObjectSearchSchema.of(
                SearchFieldTarget.USER,
                User.class,
                User.BUILTIN_PROPERTY_BINDINGS,
                columnResolver
        );
    }

    @Transactional(readOnly = true)
    public UserSearchResponse search(User actor, UserSearchRequest request) {
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

        Map<UUID, User> loaded = this.repository.userRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<UserResponse> items = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            User user = loaded.get(id);
            if (user != null) {
                items.add(UserResponse.of(user));
            }
        }

        UUID nextCursor = hasMore && !items.isEmpty() ? items.getLast().id() : null;

        this.auditService.recordSearchRead(
                AuditEntityType.USER,
                AuditService.COLLECTION_TARGET_ID,
                this.searchExecutor.summarize(request.q(), request.filters()),
                items.size()
        );

        return new UserSearchResponse(List.copyOf(items), nextCursor);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        User user = this.repository.userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user", id));

        this.auditService.addReadEvent(AuditEntityType.USER, id);
        return UserResponse.of(user);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.USER)
    public UserResponse create(NewUserRequest input) {
        if (this.repository.userRepo.findByUsername(input.username()).isPresent()) {
            throw new ResourceAlreadyExistsException("user", input.username());
        }

        AuthProvider authProvider = null;
        if (input.authProvider() != null) {
            authProvider = this.repository.authProviderRepo.findById(input.authProvider())
                    .orElseThrow(() -> new ResourceNotFoundException("authentication provider", input.authProvider()));
        }

        List<AuditPropertyChange> changes = new ArrayList<>();
        changes.add(AuditPropertyChange.newProperty("username", input.username()));
        changes.add(AuditPropertyChange.newProperty("authProvider", input.authProvider()));

        User user = new User(input.username(), authProvider);
        this.propertyApplier.applyOnCreate(user, input.properties(), true, changes);

        this.repository.userRepo.saveAndFlush(user);

        this.auditService.addEvent(
                AuditOperation.CREATE,
                AuditEntityType.USER,
                user.getId().toString(),
                changes,
                null,
                null
        );

        return UserResponse.of(user);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.USER)
    public UserResponse update(User actor, UUID id, UpdateUserRequest input) {
        User user = this.repository.userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user", id));

        List<AuditPropertyChange> changes = new ArrayList<>();

        if (input.username() != null && !input.username().equals(user.getUsername())) {
            if (this.repository.userRepo.findByUsername(input.username()).isPresent()) {
                throw new ResourceAlreadyExistsException("user", input.username());
            }

            String oldUsername = user.getUsername();
            user.setUsername(input.username());
            changes.add(AuditPropertyChange.of("username", oldUsername, input.username()));
        }

        if (input.authProvider() != null && (user.getAuthProvider() == null || !input.authProvider().equals(user.getAuthProvider().getId()))) {
            AuthProvider authProvider = this.repository.authProviderRepo.findById(input.authProvider())
                    .orElseThrow(() -> new ResourceNotFoundException("authentication provider", input.authProvider()));

            UUID oldProviderId = user.getAuthProvider() != null ? user.getAuthProvider().getId() : null;
            user.setAuthProvider(authProvider);
            changes.add(AuditPropertyChange.of("authProvider", oldProviderId, authProvider.getId()));
        }

        if (input.enabled() != null && input.enabled() != user.isEnabled()) {
            if (!input.enabled() && actor.getId().equals(user.getId())) {
                throw new ResourceInUseException("current user");
            }

            boolean oldEnabled = user.isEnabled();
            user.setEnabled(input.enabled());
            changes.add(AuditPropertyChange.of("enabled", oldEnabled, input.enabled()));

            if (!input.enabled()) {
                user.bumpSessionEpoch();
            }
        }

        if (input.properties() != null) {
            this.propertyApplier.applyOnUpdate(user, input.properties(), true, changes);
        }

        this.repository.userRepo.saveAndFlush(user);

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.USER,
                id.toString(),
                changes.isEmpty() ? null : changes,
                null,
                null
        );

        return UserResponse.of(user);
    }

    @Transactional(readOnly = true)
    public Set<ActionResponse> listActions(User actor, UUID targetUserId) {
        User target = this.repository.userRepo.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("user", targetUserId));

        UserActionContextImpl context = new UserActionContextImpl(
                this.repository,
                this.catalog,
                this.config,
                this.auditService,
                actor,
                target
        );

        Set<ActionResponse> actions = this.catalog.getRegistry(ComponentTypes.USER_ACTION).stream()
                .filter(action -> action.isAvailable(context))
                .map(action -> ActionResponse.ofUser(this.catalog, action, this.auditPolicyService))
                .collect(Collectors.toSet());

        this.auditService.addReadEvent(AuditEntityType.USER, targetUserId);
        return actions;
    }

    @Transactional
    public void executeAction(User actor, UUID targetUserId, ResourceIdentifier actionId, Map<String, ?> inputs) {
        UserActionType<?> action = this.catalog.getRegistry(ComponentTypes.USER_ACTION).get(actionId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.USER_ACTION, actionId));

        User target = this.repository.userRepo.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("user", targetUserId));

        UserActionContextImpl context = new UserActionContextImpl(
                this.repository,
                this.catalog,
                this.config,
                this.auditService,
                actor,
                target
        );

        if (!action.isAvailable(context)) {
            throw new ActionNotAvailableException(actionId, "user", targetUserId);
        }

        this.auditPolicyService.validateCommentRequired(AuditEntityType.USER, AuditOperation.ACTION);

        parseAndExecute(action, context, inputs);

        this.auditService.addActionRanEvent(
                actionId,
                AuditEntityType.USER,
                targetUserId,
                Map.of("inputs", inputs.keySet())
        );
    }

    private static <I extends Record> void parseAndExecute(
            UserActionType<I> action,
            UserActionContextImpl context,
            Map<String, ?> inputs
    ) {
        action.execute(context, JsonSchemaValidator.toRecord(action.getInputClass(), inputs));
    }
}
