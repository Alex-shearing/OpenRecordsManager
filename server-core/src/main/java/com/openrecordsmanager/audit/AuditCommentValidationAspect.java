package com.openrecordsmanager.audit;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditCommentValidationAspect {

    private final AuditPolicyService policyService;

    public AuditCommentValidationAspect(AuditPolicyService policyService) {
        this.policyService = policyService;
    }

    @Before("@annotation(requiresAuditComment)")
    public void validateRequiresAuditComment(RequiresAuditComment requiresAuditComment) {
        if (!AuditContext.isCaptureEnabled()) {
            return;
        }

        this.policyService.validateCommentRequired(
                requiresAuditComment.targetType(),
                requiresAuditComment.operation()
        );
    }
}
