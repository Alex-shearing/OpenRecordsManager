package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.template.property.PropertyType;

import java.util.List;
import java.util.UUID;

public class BuiltinConfigs {

    // Server only settings

    public static final ConfigType<Object> DATABASE_PRIMARY = ConfigType.builder("server.database.primary", PropertyType.OBJECT)
            .name("Primary Database Connection")
            .description("The primary connection to the database, this connection will be used for read/write operations.")
            .build();

    public static final ConfigType<Object> DATABASE_READ_ONLY = ConfigType.builder("server.database.read-only", PropertyType.OBJECT)
            .name("Read-Only Database Connection")
            .description("A connection to a secondary/read-only database, this connection will only be used for read operations.")
            .build();

    public static final ConfigType<String> PLUGINS_DIRECTORY = ConfigType.builder("server.plugins.directory", PropertyType.STRING)
            .name("Plugins Load Directory")
            .defaultValue("./plugins")
            .description("Defines the directory used to load plugins from.")
            .build();

    public static final ConfigType<Boolean> PLUGINS_SKIP_SYNC = ConfigType.builder("app.plugins.skip-sync", PropertyType.BOOLEAN)
            .name("Disable Plugin Sync")
            .defaultValue(false)
            .description("Disables background plugin synchronization with the database and file store.")
            .build();

    public static final String PLUGINS_SYNC_INTERVAL_MS_KEY = "app.plugins.sync-interval-ms";
    public static final ConfigType<Long> PLUGINS_SYNC_INTERVAL_MS = ConfigType.builder(PLUGINS_SYNC_INTERVAL_MS_KEY, PropertyType.NUMBER)
            .name("Plugin Sync Interval")
            .description("Milliseconds between background checks for plugin changes in the database.")
            .defaultValue(30000L)
            .build();

    public static final ConfigType<String> AUDIT_SPOOL_DIRECTORY = ConfigType.builder("server.audit-directory", PropertyType.STRING)
            .name("Audit Directory")
            .description("Local directory for audit spool and archive files")
            .defaultValue("./data/audit")
            .build();

    public static final ConfigType<String> WEB_DIRECTORY = ConfigType.builder("server.web-directory", PropertyType.STRING)
            .name("Web Client Directory")
            .description("Optional static SPA directory (e.g. ./static after unpacking orm-web-static-*.zip), leave empty when the UI is hosted separately (nginx/IIS/etc)")
            .defaultValue("./static")
            .build();

    public static final ConfigType<String> MULTIPART_MAX_FILE_SIZE = ConfigType.builder(
                    "server.servlet.multipart.max-file-size",
                    PropertyType.STRING
            )
            .name("Multipart Max File Size")
            .description("Maximum size of a single uploaded file (plugin JARs, record files). Spring Boot size format, e.g. 50MB.")
            .defaultValue("50MB")
            .build();

    public static final ConfigType<String> MULTIPART_MAX_REQUEST_SIZE = ConfigType.builder(
                    "server.servlet.multipart.max-request-size",
                    PropertyType.STRING
            )
            .name("Multipart Max Request Size")
            .description("Maximum size of a multipart HTTP request. Spring Boot size format, e.g. 50MB.")
            .defaultValue("50MB")
            .build();

    // Database ony settings

    public static final ConfigType<String> WORKGROUP_NAME = ConfigType.builder("workgroup.name", PropertyType.STRING)
            .name("Workgroup Name")
            .description("The name of the workgroup")
            .build();

    public static final ConfigType<UUID> DEFAULT_FILE_STORE = ConfigType.builder("workgroup.default_file_store", PropertyType.UUID)
            .name("Default File Store")
            .description("Sets the default store used to store new files.")
            .build();

    // Either server or database settings

    public static final ConfigType<Boolean> DEBUG_DETAILED_ERRORS = ConfigType.builder("app.debug.detailed-errors", PropertyType.BOOLEAN)
            .name("Return Detailed API Errors")
            .description("If enabled, the API will return detailed errors when they occur. This should only be enabled for debugging.")
            .defaultValue(false)
            .build();

    public static final ConfigType<Boolean> DEBUG_SHOW_SQL = ConfigType.builder("app.debug.show-sql", PropertyType.BOOLEAN)
            .name("Print All SQL to the Log")
            .description("If enabled, the API will log all SQL queries to the local log file")
            .defaultValue(false)
            .build();

    public static final String DATABASE_PROBE_INTERVAL_MS_KEY = "app.database.probe-interval-ms";
    public static final ConfigType<Long> DATABASE_PROBE_INTERVAL_MS = ConfigType.builder(DATABASE_PROBE_INTERVAL_MS_KEY, PropertyType.NUMBER)
            .name("Database Health Probe Interval")
            .description("Defines how frequently the application probes the primary database to update its health status. This is used for the audit process to ensure the database is ready to accept new audit events")
            .defaultValue(30000L)
            .build();

    public static final ConfigType<List<String>> CORS_ALLOWED_ORIGINS = ConfigType.builder("app.security.cors.allowed-origins", PropertyType.STRING_LIST)
            .name("Cross Origin Resource Sharing Allowed Origins")
            .description("Origins that should be allowed to request resources from the API, this is your web client URL(s)")
            .defaultValue(List.of("http://localhost:5173", "http://localhost:3000"))
            .build();

    public static final ConfigType<List<String>> CORS_ALLOWED_HEADERS = ConfigType.builder("app.security.cors.allowed-headers", PropertyType.STRING_LIST)
            .name("Cross Origin Resource Sharing Allowed Headers")
            .description("Headers that should be allowed to when accessing the API")
            .defaultValue(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Client-Platform", "X-ORM-Audit-Comment"))
            .build();

    public static final ConfigType<Long> TOKEN_EXPIRATION_TIME = ConfigType.builder("app.security.token-expiration-time", PropertyType.NUMBER)
            .name("Token Expiration Time")
            .description("How long issued authentication tokens are valid for after issuing")
            .defaultValue(3600000L)
            .build();

    public static final ConfigType<String> COOKIE_NAME = ConfigType.builder("app.security.cookie-auth.name", PropertyType.STRING)
            .name("Cookie Name")
            .description("When using cookie-based authentication, the name of the cookie to use")
            .defaultValue("ORM-Authentication")
            .build();

    public static final ConfigType<Boolean> COOKIE_SECURE = ConfigType.builder("app.security.cookie-auth.secure", PropertyType.BOOLEAN)
            .name("Cookie Secure Only")
            .description("When using cookie-based authentication, enforce the 'Secure' attribute")
            .defaultValue(true)
            .build();

    // Web UI branding

    public static final ConfigType<String> WEB_PRODUCT_NAME = ConfigType.builder("app.web.product-name", PropertyType.STRING)
            .name("Product Name")
            .description("Product name shown in the web client.")
            .defaultValue("Open Records Manager")
            .build();

    public static final ConfigType<String> WEB_LOGO_URL = ConfigType.builder("app.web.logo-url", PropertyType.STRING)
            .name("Logo URL")
            .description("Optional logo image URL for the web client.")
            .defaultValue("")
            .build();

    public static final ConfigType<String> WEB_FAVICON_URL = ConfigType.builder("app.web.favicon-url", PropertyType.STRING)
            .name("Favicon URL")
            .description("Optional favicon URL for the web client.")
            .defaultValue("/favicon.ico")
            .build();

    public static final ConfigType<String> WEB_PRIMARY_COLOR = ConfigType.builder("app.web.primary-color", PropertyType.STRING)
            .name("Primary Color")
            .description("Primary brand color (CSS) for the web client.")
            .defaultValue("#1d4ed8")
            .build();

    public static final ConfigType<String> WEB_SUPPORT_URL = ConfigType.builder("app.web.support-url", PropertyType.STRING)
            .name("Support URL")
            .description("Optional support link shown in the web client.")
            .defaultValue("")
            .build();

    // Audit settings

    public static final ConfigType<Boolean> AUDIT_ENABLED = ConfigType.builder("app.audit.enabled", PropertyType.BOOLEAN)
            .name("Audit Enabled")
            .description("Master switch for the audit system")
            .defaultValue(true)
            .build();

    public static final ConfigType<Long> AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS = ConfigType.builder(
                    "app.audit.spool-drain-interval-seconds",
                    PropertyType.NUMBER
            )
            .name("Audit Spool Drain Interval")
            .description("Seconds between background attempts to drain the audit spool into the database.")
            .defaultValue(30L)
            .build();

    public static final ConfigType<Boolean> AUDIT_FILE_ARCHIVE_ENABLED = ConfigType.builder(
                    "app.audit.archive-enabled",
                    PropertyType.BOOLEAN
            )
            .name("Audit Archive Log Enabled")
            .description("When enabled, synced audit events are also appended to a daily rotating archive log file.")
            .defaultValue(true)
            .build();

}
