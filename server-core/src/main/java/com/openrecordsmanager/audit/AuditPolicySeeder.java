package com.openrecordsmanager.audit;

import com.openrecordsmanager.database.DatabaseWritableProbe;
import com.openrecordsmanager.database.schema.SchemaMigrationReadyEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class AuditPolicySeeder {

    private final AuditPolicyService policyService;
    private final DatabaseWritableProbe probe;

    public AuditPolicySeeder(AuditPolicyService policyService, DatabaseWritableProbe probe) {
        this.policyService = policyService;
        this.probe = probe;
    }

    @Order(1)
    @EventListener({ApplicationReadyEvent.class, SchemaMigrationReadyEvent.class})
    public void seedWhenSchemaReady() {
        if (!this.probe.isWritable()) {
            return;
        }
        this.policyService.seedEntityPolicies();
    }
}
