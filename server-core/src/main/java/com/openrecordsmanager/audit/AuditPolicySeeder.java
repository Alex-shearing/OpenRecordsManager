package com.openrecordsmanager.audit;

import com.openrecordsmanager.database.schema.SchemaMigrationReadyEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuditPolicySeeder {

    private final AuditPolicyService policyService;

    public AuditPolicySeeder(AuditPolicyService policyService) {
        this.policyService = policyService;
    }

    @EventListener({ApplicationReadyEvent.class, SchemaMigrationReadyEvent.class})
    public void seedWhenSchemaReady() {
        this.policyService.seedEntityPolicies();
    }
}
