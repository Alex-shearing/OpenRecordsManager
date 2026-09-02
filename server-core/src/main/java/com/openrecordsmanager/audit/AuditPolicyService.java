package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.audit.persistence.AuditPolicyEntity;
import com.openrecordsmanager.audit.persistence.AuditPolicyId;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.schema.SchemaMigrationState;
import com.openrecordsmanager.rest.errors.AuditCommentRequiredException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AuditPolicyService {

    private final ConfigService config;
    private final DataRepository repository;
    private final SchemaMigrationState migrationState;

    public AuditPolicyService(
            ConfigService config,
            DataRepository repository,
            SchemaMigrationState migrationState
    ) {
        this.config = config;
        this.repository = repository;
        this.migrationState = migrationState;
    }

    public boolean isAuditDisabled() {
        return getAuditDisabledReason() != null;
    }

    public @Nullable String getAuditDisabledReason() {
        if (this.migrationState.isUpgradeRequired()) {
            return "schema_migration_required";
        }
        if (!this.config.getOptional(BuiltinConfigs.AUDIT_ENABLED).orElse(true)) {
            return "disabled_by_config";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public boolean isEventEnabled(AuditEntityType entityType, AuditOperation operation) {
        if (this.isAuditDisabled()) {
            return false;
        }
        return this.repository.auditPolicyRepo.findById(new AuditPolicyId(entityType, operation))
                .map(policy -> policy.enabled)
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public boolean requiresComment(AuditEntityType entityType, AuditOperation operation) {
        if (this.isAuditDisabled()) {
            return false;
        }
        return this.repository.auditPolicyRepo.findById(new AuditPolicyId(entityType, operation))
                .map(policy -> policy.requiresComment)
                .orElse(false);
    }

    public void validateCommentRequired(AuditEntityType entityType, AuditOperation operation) {
        if (this.requiresComment(entityType, operation) && AuditContext.comment().isEmpty()) {
            throw new AuditCommentRequiredException();
        }
    }

    @Transactional
    public void ensurePolicyExists(
            AuditEntityType entityType,
            AuditOperation operation,
            String displayName,
            @Nullable String description
    ) {
        AuditPolicyId id = new AuditPolicyId(entityType, operation);
        if (this.repository.auditPolicyRepo.existsById(id)) {
            return;
        }
        this.repository.auditPolicyRepo.saveAndFlush(new AuditPolicyEntity(
                id,
                true,
                false,
                displayName,
                description
        ));
    }

    @Transactional
    public void seedEntityPolicies() {
        if (this.isAuditDisabled()) {
            return;
        }

        for (AuditEntityType entityType : AuditEntityType.values()) {
            String entityName = entityType.key();
            this.ensurePolicyExists(
                    entityType,
                    AuditOperation.READ,
                    entityName + " read",
                    "Automatic audit when a " + entityName + " is read"
            );
            this.ensurePolicyExists(
                    entityType,
                    AuditOperation.CREATE,
                    entityName + " created",
                    "Automatic audit when a " + entityName + " is created"
            );
            this.ensurePolicyExists(
                    entityType,
                    AuditOperation.UPDATE,
                    entityName + " updated",
                    "Automatic audit when a " + entityName + " is updated"
            );
            this.ensurePolicyExists(
                    entityType,
                    AuditOperation.DELETE,
                    entityName + " deleted",
                    "Automatic audit when a " + entityName + " is deleted"
            );
            this.ensurePolicyExists(
                    entityType,
                    AuditOperation.ACTION,
                    "ran action on " + entityName,
                    "Automatic audit when an action is ran on a " + entityName
            );
        }
    }

    @Transactional(readOnly = true)
    public List<AuditPolicyEntity> getAllPolicies() {
        return this.repository.auditPolicyRepo.findAll();
    }

    @Transactional
    public AuditPolicyEntity updatePolicy(
            AuditEntityType entityType,
            AuditOperation operation,
            boolean enabled,
            boolean requiresComment
    ) {
        AuditPolicyEntity policy = this.repository.auditPolicyRepo.findById(new AuditPolicyId(entityType, operation))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown audit policy: " + entityType.key() + " / " + operation
                ));
        policy.enabled = enabled;
        policy.requiresComment = requiresComment;
        return this.repository.auditPolicyRepo.saveAndFlush(policy);
    }

    @Transactional(readOnly = true)
    public Optional<AuditPolicyEntity> findPolicy(AuditEntityType entityType, AuditOperation operation) {
        return this.repository.auditPolicyRepo.findById(new AuditPolicyId(entityType, operation));
    }
}
