package com.openrecordsmanager.auth;

import org.jspecify.annotations.Nullable;

/**
 * Validates relative paths used for post-login redirects to prevent open redirects.
 */
public final class SafeRelativePaths {

    private SafeRelativePaths() {
    }

    /**
     * @return true if {@code path} is a same-origin relative path starting with {@code /}
     * but not {@code //} (protocol-relative) and without backslashes
     */
    public static boolean isSafe(@Nullable String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (!path.startsWith("/") || path.startsWith("//")) {
            return false;
        }
        if (path.indexOf('\\') >= 0) {
            return false;
        }
        // Reject encoded tricks that could escape the path
        String lower = path.toLowerCase();
        return !lower.contains("%2f%2f") && !lower.contains("%5c");
    }

    /**
     * @return {@code path} if safe, otherwise {@code fallback}
     */
    public static String orElse(@Nullable String path, String fallback) {
        return isSafe(path) ? path : fallback;
    }

    public static @Nullable String sanitizeOrNull(@Nullable String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return isSafe(path) ? path : null;
    }
}
