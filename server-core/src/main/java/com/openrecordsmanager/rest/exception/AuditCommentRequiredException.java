package com.openrecordsmanager.rest.exception;

import com.openrecordsmanager.api.errors.ApiException;

public class AuditCommentRequiredException extends ApiException {
    public AuditCommentRequiredException() {
        super("audit_comment_required");
    }
}
