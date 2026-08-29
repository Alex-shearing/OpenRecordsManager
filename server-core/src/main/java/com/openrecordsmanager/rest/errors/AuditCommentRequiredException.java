package com.openrecordsmanager.rest.errors;

public class AuditCommentRequiredException extends RuntimeException {
    public AuditCommentRequiredException() {
        super("An audit comment is required for this action (provide the X-ORM-Audit-Comment header)");
    }
}
