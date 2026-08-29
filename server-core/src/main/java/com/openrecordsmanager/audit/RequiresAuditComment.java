package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates {@link AuditContext#COMMENT_HEADER} when the matching audit policy requires a comment.
 * Event key is derived as {@code entity:{targetType}:{operation}}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAuditComment {

    AuditOperation operation();

    AuditEntityType targetType();
}
