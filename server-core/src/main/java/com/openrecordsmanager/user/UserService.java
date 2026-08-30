package com.openrecordsmanager.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
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
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.rest.dto.ActionResponse;
import com.openrecordsmanager.rest.errors.ResourceInUseException;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import com.openrecordsmanager.user.dto.NewUserRequest;
import com.openrecordsmanager.user.dto.UpdateUserRequest;
import com.openrecordsmanager.user.dto.UserResponse;
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

    public UserService(
            DataRepository repository,
            ConfigService config,
            ComponentCatalog catalog,
            AuditService auditService,
            AuditPolicyService auditPolicyService
    ) {
        this.repository = repository;
        this.config = config;
        this.catalog = catalog;
        this.auditService = auditService;
        this.auditPolicyService = auditPolicyService;
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
            throw new ResourceInUseException("user already exists: " + input.username());
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
        input.properties().forEach((identifier, value) -> {
            ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(identifier)
                    .filter(p -> !p.isUserHidden())
                    .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, identifier));

            Object newValue = user.setPropertyUntyped(property, value);
            changes.add(AuditPropertyChange.newProperty(identifier.toString(), newValue));
        });

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
    public UserResponse update(UUID id, UpdateUserRequest input) {
        User user = this.repository.userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user", id));

        List<AuditPropertyChange> changes = new ArrayList<>();

        if (input.username() != null && !input.username().equals(user.getUsername())) {
            if (this.repository.userRepo.findByUsername(input.username()).isPresent()) {
                throw new ResourceInUseException("user already exists: " + input.username());
            }

            String oldUsername = user.getUsername();
            user.setUsername(input.username());
            changes.add(new AuditPropertyChange("username", oldUsername, input.username()));
        }

        if (input.authProvider() != null && (user.getAuthProvider() == null || input.authProvider() != user.getAuthProvider().getId())) {
            AuthProvider authProvider = this.repository.authProviderRepo.findById(input.authProvider())
                    .orElseThrow(() -> new ResourceNotFoundException("authentication provider", input.authProvider()));

            UUID oldProviderId = user.getAuthProvider() != null ? user.getAuthProvider().getId() : null;
            user.setAuthProvider(authProvider);
            changes.add(new AuditPropertyChange("authProvider", oldProviderId, authProvider.getId()));
        }

        if (input.properties() != null) {
            input.properties().forEach((identifier, value) -> {
                ObjectProperty<?> property = this.repository.objectPropertyRepo.findById(identifier)
                        .filter(p -> !p.isUserHidden())
                        .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.OBJECT_PROPERTY, identifier));

                Object oldValue = user.getProperty(property);
                Object newValue = user.setPropertyUntyped(property, value);
                if (oldValue != newValue) {
                    changes.add(new AuditPropertyChange(identifier.toString(), oldValue, newValue));
                }
            });
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
            throw new IllegalArgumentException("Action " + actionId + " is not available for user " + targetUserId);
        }

        this.auditPolicyService.validateCommentRequired(AuditEntityType.USER, AuditOperation.ACTION);

        action.executeUntyped(context, inputs);

        this.auditService.addActionRanEvent(
                actionId,
                AuditEntityType.USER,
                targetUserId,
                Map.of("inputs", inputs.keySet())
        );
    }
}
