package com.openrecordsmanager.audit;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class AuditContext {

    public static final String COMMENT_HEADER = "X-ORM-Audit-Comment";

    private static final ThreadLocal<@Nullable State> CURRENT = new ThreadLocal<>();

    private AuditContext() {
    }

    public static void begin(@Nullable UUID actorId, @Nullable String actorUsername, @Nullable String comment, boolean captureEnabled) {
        CURRENT.set(new State(actorId, actorUsername, comment, captureEnabled));
    }

    public static void disableCapture() {
        State state = CURRENT.get();
        if (state != null) {
            CURRENT.set(state.withCaptureDisabled());
        }
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static boolean isCaptureEnabled() {
        State state = CURRENT.get();
        return state == null || state.captureEnabled();
    }

    public static Optional<UUID> actorId() {
        State state = CURRENT.get();
        return state == null ? Optional.empty() : Optional.ofNullable(state.actorId);
    }

    public static Optional<String> actorUsername() {
        State state = CURRENT.get();
        return state == null ? Optional.empty() : Optional.ofNullable(state.actorUsername);
    }

    public static Optional<String> comment() {
        return Optional.ofNullable(CURRENT.get()).map(State::comment).filter(s -> !s.isBlank());
    }

    private record State(
            @Nullable UUID actorId,
            @Nullable String actorUsername,
            @Nullable String comment,
            boolean captureEnabled
    ) {
        State withCaptureDisabled() {
            return new State(this.actorId, this.actorUsername, this.comment, false);
        }
    }
}
