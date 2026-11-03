package com.openrecordsmanager.action;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.action.ActionContext;
import com.openrecordsmanager.api.action.ActionType;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ManagedObject;
import com.openrecordsmanager.rest.dto.ActionResponse;
import com.openrecordsmanager.rest.exception.ActionNotAvailableException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.schema.JsonSchemaValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ActionService {

    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final ConfigService config;
    private final AuditService auditService;
    private final AuditPolicyService auditPolicyService;

    public ActionService(
            DataRepository repository,
            ComponentCatalog catalog,
            ConfigService config,
            AuditService auditService,
            AuditPolicyService auditPolicyService
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.config = config;
        this.auditService = auditService;
        this.auditPolicyService = auditPolicyService;
    }

    @Transactional(readOnly = true)
    public Set<ActionResponse> listActions(User actor, ManagedObject<?> target, AuditEntityType entityType) {
        ActionContext context = this.actionContext(actor, target, entityType);

        Set<ActionResponse> actions = this.catalog.getRegistry(ComponentTypes.ACTION).stream()
                .filter(action -> action.isAvailable(context))
                .map(action -> ActionResponse.of(
                        this.catalog,
                        action,
                        entityType,
                        this.auditPolicyService
                ))
                .collect(Collectors.toSet());

        this.auditService.addReadEvent(entityType, target.getId());
        return actions;
    }

    @Transactional
    public void executeAction(
            User actor,
            ManagedObject<?> target,
            AuditEntityType entityType,
            ResourceIdentifier actionId,
            Map<String, ?> inputs
    ) {
        ActionType<?> action = this.catalog.getRegistry(ComponentTypes.ACTION).get(actionId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.ACTION, actionId));

        ActionContext context = this.actionContext(actor, target, entityType);

        if (!action.isAvailable(context)) {
            throw new ActionNotAvailableException(actionId, entityType.key(), target.getId());
        }

        this.auditPolicyService.validateCommentRequired(entityType, AuditOperation.ACTION);

        parseAndExecute(action, context, inputs);

        this.auditService.addActionRanEvent(
                actionId,
                entityType,
                target.getId(),
                Map.of("inputs", inputs.keySet())
        );
    }

    private ActionContext actionContext(User actor, ManagedObject<?> target, AuditEntityType entityType) {
        return new ActionContextImpl(
                this.repository,
                this.catalog,
                this.config,
                this.auditService,
                actor,
                target,
                entityType
        );
    }

    private static <I extends Record> void parseAndExecute(
            ActionType<I> action,
            ActionContext context,
            Map<String, ?> inputs
    ) {
        action.execute(context, JsonSchemaValidator.toRecord(action.getInputClass(), inputs));
    }
}
