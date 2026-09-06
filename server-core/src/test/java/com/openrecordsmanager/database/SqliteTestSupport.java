package com.openrecordsmanager.database;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SQLite helpers for tests. Anonymous {@code jdbc:sqlite::memory:} is unsafe with Hikari:
 * each pooled connection gets a separate empty database. Named shared-memory URLs keep one DB
 * per logical name across the pool.
 * <p>
 * Each test class gets a unique name for the JVM lifetime so a destroyed Spring context cannot
 * leave a half-seeded schema for the next context that reuses the same class configuration.
 */
public final class SqliteTestSupport {

    private static final ConcurrentMap<String, String> URLS_BY_NAME = new ConcurrentHashMap<>();

    private SqliteTestSupport() {
    }

    public static void registerPrimaryMemoryDatabase(DynamicPropertyRegistry registry, Class<?> testClass) {
        registerPrimaryMemoryDatabase(registry, testClass.getSimpleName());
    }

    public static void registerPrimaryMemoryDatabase(DynamicPropertyRegistry registry, String name) {
        String url = URLS_BY_NAME.computeIfAbsent(
                name,
                key -> "jdbc:sqlite:file:orm-" + key + "-" + UUID.randomUUID() + "?mode=memory&cache=shared"
        );
        registry.add("server.database.primary.url", () -> url);
        registry.add("server.database.primary.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("server.database.read-only.url", () -> "");
    }
}
