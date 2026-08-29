package com.openrecordsmanager.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.api.user.UserActionType;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.dto.ActionResponse;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
