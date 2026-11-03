package com.openrecordsmanager.rest.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.action.ActionType;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.schema.InputFormSchema;
import jakarta.validation.constraints.NotNull;

public record ActionResponse(
        @NotNull ResourceIdentifier id,
        @NotNull InputFormSchema inputSchema,
        boolean requiresAuditComment
) {

    public static ActionResponse of(
            ComponentCatalog catalog,
            ActionType<?> action,
            AuditEntityType entityType,
            AuditPolicyService auditPolicyService
    ) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.ACTION).getId(action)
                .orElseThrow();

        return new ActionResponse(
                id,
                InputFormSchema.from(action.getInputClass()),
                auditPolicyService.requiresComment(entityType, AuditOperation.ACTION)
        );
    }
}
