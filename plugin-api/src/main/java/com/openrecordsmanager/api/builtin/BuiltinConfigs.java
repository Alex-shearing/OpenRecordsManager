package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.template.property.PropertyType;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

public class BuiltinConfigs {

    // Server only settings

    public static final ConfigType<JsonNode> DATABASE_PRIMARY = ConfigType.builder("server.database.primary", PropertyType.OBJECT)
            .build();

    public static final ConfigType<JsonNode> DATABASE_READ_ONLY = ConfigType.builder("server.database.read-only", PropertyType.OBJECT)
            .build();

    public static final ConfigType<String> PLUGINS_DIRECTORY = ConfigType.builder("server.plugins.directory", PropertyType.STRING)
            .defaultValue("./plugins")
            .build();

    public static final ConfigType<Boolean> PLUGINS_SKIP_SYNC = ConfigType.builder("app.plugins.skip-sync", PropertyType.BOOLEAN)
            .defaultValue(false)
            .build();

    public static final String PLUGINS_SYNC_INTERVAL_MS_KEY = "app.plugins.sync-interval-ms";
    public static final ConfigType<Long> PLUGINS_SYNC_INTERVAL_MS = ConfigType.builder(PLUGINS_SYNC_INTERVAL_MS_KEY, PropertyType.NUMBER)
            .defaultValue(30000L)
            .build();

    public static final ConfigType<String> AUDIT_SPOOL_DIRECTORY = ConfigType.builder("server.audit-directory", PropertyType.STRING)
            .defaultValue("./data/audit")
            .build();

    public static final ConfigType<String> WEB_DIRECTORY = ConfigType.builder("server.web-directory", PropertyType.STRING)
            .defaultValue("./static")
            .build();

    public static final ConfigType<String> MULTIPART_MAX_FILE_SIZE = ConfigType.builder(
                    "server.servlet.multipart.max-file-size",
                    PropertyType.STRING
            )
            .defaultValue("50MB")
            .build();

    public static final ConfigType<String> MULTIPART_MAX_REQUEST_SIZE = ConfigType.builder(
                    "server.servlet.multipart.max-request-size",
                    PropertyType.STRING
            )
            .defaultValue("50MB")
            .build();

    public static final ConfigType<String> JWT_SIGNING_KEY = ConfigType.builder("server.security.jwt-signing-key", PropertyType.STRING)
            .defaultValue("orm-dev-only-change-me-in-production-32b")
            .build();

    // Database ony settings

    public static final ConfigType<String> WORKGROUP_NAME = ConfigType.builder("workgroup.name", PropertyType.STRING)
            .build();

    public static final ConfigType<UUID> DEFAULT_FILE_STORE = ConfigType.builder("workgroup.default_file_store", PropertyType.UUID)
            .build();

    // Either server or database settings

    public static final ConfigType<Boolean> DEBUG_DETAILED_ERRORS = ConfigType.builder("app.debug.detailed-errors", PropertyType.BOOLEAN)
            .defaultValue(false)
            .build();

    public static final ConfigType<Boolean> DEBUG_SHOW_SQL = ConfigType.builder("app.debug.show-sql", PropertyType.BOOLEAN)
            .defaultValue(false)
            .build();

    public static final String DATABASE_PROBE_INTERVAL_MS_KEY = "app.database.probe-interval-ms";
    public static final ConfigType<Long> DATABASE_PROBE_INTERVAL_MS = ConfigType.builder(DATABASE_PROBE_INTERVAL_MS_KEY, PropertyType.NUMBER)
            .defaultValue(30000L)
            .build();

    public static final ConfigType<List<String>> CORS_ALLOWED_ORIGINS = ConfigType.builder("app.security.cors.allowed-origins", PropertyType.STRING_LIST)
            .defaultValue(List.of())
            .build();

    public static final ConfigType<List<String>> CORS_ALLOWED_HEADERS = ConfigType.builder("app.security.cors.allowed-headers", PropertyType.STRING_LIST)
            .defaultValue(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN", "X-Client-Platform", "X-ORM-Audit-Comment", "Accept-Language"))
            .build();

    public static final ConfigType<Long> ACCESS_TOKEN_EXPIRATION_SECONDS = ConfigType.builder(
                    "app.security.access-token-expiration-seconds",
                    PropertyType.NUMBER
            )
            .defaultValue(3600L)
            .build();

    public static final ConfigType<Long> DEGRADED_ACCESS_TOKEN_EXPIRATION_SECONDS = ConfigType.builder(
                    "app.security.degraded-access-token-expiration-seconds",
                    PropertyType.NUMBER
            )
            .defaultValue(900L)
            .build();

    public static final ConfigType<Long> REFRESH_TOKEN_EXPIRATION_SECONDS = ConfigType.builder(
                    "app.security.refresh-token-expiration-seconds",
                    PropertyType.NUMBER
            )
            .defaultValue(604800L)
            .build();

    public static final ConfigType<Long> DEGRADED_REFRESH_TOKEN_EXPIRATION_SECONDS = ConfigType.builder(
                    "app.security.degraded-refresh-token-expiration-seconds",
                    PropertyType.NUMBER
            )
            .defaultValue(28800L)
            .build();

    public static final ConfigType<String> COOKIE_NAME = ConfigType.builder("app.security.cookie-auth.name", PropertyType.STRING)
            .defaultValue("ORM-Authentication")
            .build();

    public static final ConfigType<String> REFRESH_COOKIE_NAME = ConfigType.builder("app.security.cookie-auth.refresh-name", PropertyType.STRING)
            .defaultValue("ORM-Refresh-Authentication")
            .build();

    public static final ConfigType<Boolean> COOKIE_SECURE = ConfigType.builder("app.security.cookie-auth.secure", PropertyType.BOOLEAN)
            .defaultValue(true)
            .build();

    public static final ConfigType<String> PUBLIC_BASE_URL = ConfigType.builder("app.security.public-base-url", PropertyType.STRING)
            .defaultValue("http://localhost:8080")
            .build();

    // Web UI branding

    public static final ConfigType<String> WEB_LOGO_URL = ConfigType.builder("app.web.logo-url", PropertyType.STRING)
            .defaultValue("")
            .build();

    public static final ConfigType<String> WEB_FAVICON_URL = ConfigType.builder("app.web.favicon-url", PropertyType.STRING)
            .defaultValue("")
            .build();

    public static final ConfigType<String> WEB_PRIMARY_COLOR = ConfigType.builder("app.web.primary-color", PropertyType.STRING)
            .defaultValue("#1d4ed8")
            .build();

    public static final ConfigType<String> WEB_SUPPORT_URL = ConfigType.builder("app.web.support-url", PropertyType.STRING)
            .defaultValue("")
            .build();

    // Audit settings

    public static final ConfigType<Boolean> AUDIT_ENABLED = ConfigType.builder("app.audit.enabled", PropertyType.BOOLEAN)
            .defaultValue(true)
            .build();

    public static final ConfigType<Long> AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS = ConfigType.builder(
                    "app.audit.spool-drain-interval-seconds",
                    PropertyType.NUMBER
            )
            .defaultValue(30L)
            .build();

    public static final ConfigType<Boolean> AUDIT_FILE_ARCHIVE_ENABLED = ConfigType.builder(
                    "app.audit.archive-enabled",
                    PropertyType.BOOLEAN
            )
            .defaultValue(true)
            .build();

}
