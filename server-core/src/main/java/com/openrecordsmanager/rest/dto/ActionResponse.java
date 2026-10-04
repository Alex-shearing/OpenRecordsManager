package com.openrecordsmanager.rest.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.location.LocationActionType;
import com.openrecordsmanager.api.record.RecordActionType;
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

    public static ActionResponse ofLocation(
            ComponentCatalog catalog,
            LocationActionType<?> action,
            AuditPolicyService auditPolicyService
    ) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.LOCATION_ACTION).getId(action)
                .orElseThrow();

        return new ActionResponse(
                id,
                InputFormSchema.from(action.getInputClass()),
                auditPolicyService.requiresComment(AuditEntityType.LOCATION, AuditOperation.ACTION)
        );
    }

    public static ActionResponse ofRecord(
            ComponentCatalog catalog,
            RecordActionType<?> action,
            AuditPolicyService auditPolicyService
    ) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.RECORD_ACTION).getId(action)
                .orElseThrow();

        return new ActionResponse(
                id,
                InputFormSchema.from(action.getInputClass()),
                auditPolicyService.requiresComment(AuditEntityType.RECORD, AuditOperation.ACTION)
        );
    }
}
