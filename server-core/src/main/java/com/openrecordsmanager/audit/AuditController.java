package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.audit.dto.AuditEventResponse;
import com.openrecordsmanager.audit.dto.AuditPolicyResponse;
import com.openrecordsmanager.audit.dto.AuditStatusResponse;
import com.openrecordsmanager.audit.dto.UpdateAuditPolicyRequest;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import com.openrecordsmanager.user.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class AuditController {

    private final AuditQueryService queryService;

    public AuditController(AuditQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping(value = "/events", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List audit events for an entity")
    public List<AuditEventResponse> listAuditEvents(
            @AuthenticationPrincipal User user,
            @RequestParam("targetType") String targetType,
            @RequestParam("targetId") String targetId,
            @RequestParam(value = "before", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Nullable Instant before,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return this.queryService.listAuditEvents(
                user,
                AuditEntityType.fromKey(targetType),
                targetId,
                before,
                limit
        );
    }

    @GetMapping(value = "/events/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a single audit event")
    @NotFoundApiResponse
    public AuditEventResponse getAuditEvent(@AuthenticationPrincipal User user, @PathVariable("id") UUID id) {
        return this.queryService.getAuditEvent(user, id);
    }

    @GetMapping(value = "/policies", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List audit policies")
    public List<AuditPolicyResponse> listAuditPolicies() {
        return this.queryService.listAuditPolicies();
    }

    @PutMapping(value = "/policies", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update an audit policy")
    @NotFoundApiResponse
    public AuditPolicyResponse updateAuditPolicy(
            @RequestParam("entityType") String entityType,
            @RequestParam("operation") AuditOperation operation,
            @Valid @RequestBody UpdateAuditPolicyRequest request
    ) {
        return this.queryService.updateAuditPolicy(AuditEntityType.fromKey(entityType), operation, request);
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get audit subsystem status for the current server")
    public AuditStatusResponse getAuditStatus() {
        return this.queryService.getAuditStatus();
    }
}
