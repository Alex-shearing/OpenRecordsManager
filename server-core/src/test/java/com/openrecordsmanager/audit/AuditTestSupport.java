package com.openrecordsmanager.audit;

import com.openrecordsmanager.location.user.User;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Test helper for calling {@code @RequiresAuditComment} service methods outside the HTTP filter chain.
 */
public final class AuditTestSupport {

    private AuditTestSupport() {
    }

    /**
     * Runs {@code action} with a capture-enabled {@link AuditContext} and a default comment of {@code "test"}.
     */
    public static <T> T withAudit(User actor, Callable<T> action) {
        return withAudit(actor, "test", action);
    }

    /**
     * Runs {@code action} with a capture-enabled {@link AuditContext} for the given actor and comment.
     */
    public static <T> T withAudit(User actor, @Nullable String comment, Callable<T> action) {
        return withAudit(actor.getId(), actor.getUsername(), comment, action);
    }

    /**
     * Runs {@code action} with a capture-enabled {@link AuditContext}.
     */
    public static <T> T withAudit(
            @Nullable UUID actorId,
            @Nullable String actorUsername,
            @Nullable String comment,
            Callable<T> action
    ) {
        AuditContext.begin(actorId, actorUsername, comment, true);
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            AuditContext.clear();
        }
    }
}
