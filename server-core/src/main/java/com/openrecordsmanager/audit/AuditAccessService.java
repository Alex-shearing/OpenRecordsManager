package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import com.openrecordsmanager.user.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditAccessService {

    private final DataRepository repository;
    private final ExpressionsService expressions;

    public AuditAccessService(DataRepository repository, ExpressionsService expressions) {
        this.repository = repository;
        this.expressions = expressions;
    }

    public void assertCanViewTarget(User actor, AuditEntityType targetType, String targetId) {
        switch (targetType) {
            case RECORD -> this.assertCanViewRecord(actor, UUID.fromString(targetId));
            case USER -> this.assertCanViewUser(actor, UUID.fromString(targetId));
            default -> {
                // Configuration entities are visible to authenticated users for now.
            }
        }
    }

    private void assertCanViewRecord(User actor, UUID recordId) {
        this.repository.recordRepo.findById(recordId)
                .filter(record -> record.securityFilter(this.expressions, actor).canSeeMetadata())
                .orElseThrow(() -> new ResourceNotFoundException("record", recordId));
    }

    private void assertCanViewUser(User actor, UUID userId) {
        this.repository.userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));
    }
}
