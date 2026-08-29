package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.audit.dto.AuditEventResponse;
import com.openrecordsmanager.audit.dto.AuditPolicyResponse;
import com.openrecordsmanager.audit.dto.AuditStatusResponse;
import com.openrecordsmanager.audit.dto.UpdateAuditPolicyRequest;
import com.openrecordsmanager.audit.persistence.AuditPolicyEntity;
import com.openrecordsmanager.audit.spool.AuditSpoolDrainer;
import com.openrecordsmanager.audit.spool.AuditSpoolWriter;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.DatabaseWritableProbe;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import com.openrecordsmanager.user.User;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditQueryService {

    private final DataRepository repository;
    private final AuditService auditService;
    private final AuditSpoolWriter spoolWriter;
    private final AuditAccessService accessService;
    private final AuditPolicyService policyService;
    private final DatabaseWritableProbe probe;
    private final AuditSpoolDrainer drainer;

    public AuditQueryService(
            DataRepository repository,
            AuditService auditService,
            AuditSpoolWriter spoolWriter,
            AuditAccessService accessService,
            AuditPolicyService policyService,
            DatabaseWritableProbe probe,
            AuditSpoolDrainer drainer
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.spoolWriter = spoolWriter;
        this.accessService = accessService;
        this.policyService = policyService;
        this.probe = probe;
        this.drainer = drainer;
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> listEvents(
            User actor,
            AuditEntityType targetType,
            String targetId,
            @Nullable Instant before,
            int limit
    ) {
        this.accessService.assertCanViewTarget(actor, targetType, targetId);

        int effectiveLimit = Math.clamp(limit, 1, 200);
        List<AuditEventPayload> events = new ArrayList<>();
        String targetTypeKey = targetType.key();

        if (before != null) {
            this.repository.auditEventRepo.findByTargetBefore(
                            targetTypeKey,
                            targetId,
                            before,
                            PageRequest.of(0, effectiveLimit)
                    ).stream()
                    .map(this.auditService::toPayload)
                    .forEach(events::add);
        } else {
            this.repository.auditEventRepo.findByTargetTypeAndTargetIdOrderByOccurredAtDesc(
                            targetTypeKey,
                            targetId,
                            PageRequest.of(0, effectiveLimit)
                    ).stream()
                    .map(this.auditService::toPayload)
                    .forEach(events::add);
        }

        events.addAll(this.spoolWriter.readForTarget(targetType, targetId));

        List<AuditEventResponse> responses = dedupeAndLimit(events, before, effectiveLimit).stream()
                .map(AuditEventResponse::of)
                .toList();

        this.auditService.recordSearchRead(targetType, targetId, before == null ? "" : before.toString(), responses.size());
        return responses;
    }

    @Transactional(readOnly = true)
    public AuditEventResponse getEvent(User actor, UUID eventId) {
        AuditEventPayload payload = this.repository.auditEventRepo.findById(eventId)
                .map(this.auditService::toPayload)
                .orElseGet(() -> this.spoolWriter.readPending().stream()
                        .filter(event -> event.id().equals(eventId))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("audit event", eventId)));

        this.accessService.assertCanViewTarget(actor, payload.targetType(), payload.targetId());
        this.auditService.addReadEvent(payload.targetType(), payload.targetId());
        return AuditEventResponse.of(payload);
    }

    @Transactional(readOnly = true)
    public List<AuditPolicyResponse> listPolicies() {
        return this.policyService.getAllPolicies().stream()
                .map(AuditPolicyResponse::of)
                .toList();
    }

    @Transactional
    public AuditPolicyResponse updatePolicy(
            AuditEntityType entityType,
            AuditOperation operation,
            UpdateAuditPolicyRequest request
    ) {
        AuditPolicyEntity updated = this.policyService.updatePolicy(
                entityType,
                operation,
                request.enabled(),
                request.requiresComment()
        );
        return AuditPolicyResponse.of(updated);
    }

    public AuditStatusResponse status() {
        return new AuditStatusResponse(
                this.probe.isWritable(),
                this.spoolWriter.pendingCount(),
                this.probe.getLastChecked(),
                this.probe.getLastSuccessfulWrite(),
                this.drainer.getLastDrainAttempt(),
                this.drainer.getLastSuccessfulDrain()
        );
    }

    private static List<AuditEventPayload> dedupeAndLimit(
            List<AuditEventPayload> events,
            @Nullable Instant before,
            int limit
    ) {
        Map<UUID, AuditEventPayload> deduped = events.stream()
                .sorted(Comparator.comparing(AuditEventPayload::occurredAt).reversed())
                .collect(Collectors.toMap(
                        AuditEventPayload::id,
                        event -> event,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return deduped.values().stream()
                .filter(event -> before == null || event.occurredAt().isBefore(before))
                .limit(limit)
                .toList();
    }
}
