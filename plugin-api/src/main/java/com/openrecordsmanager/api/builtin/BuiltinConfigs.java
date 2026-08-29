package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.config.ConfigValueType;

import java.util.UUID;

public class BuiltinConfigs {

    // Server only settings

    public static final ConfigType<Object> DATABASE_PRIMARY = ConfigType.builder("server.database.primary", ConfigValueType.OBJECT)
            .name("Primary Database Connection")
            .description("The primary connection to the database, this connection will be used for read/write operations.")
            .build();

    public static final ConfigType<Object> DATABASE_READ_ONLY = ConfigType.builder("server.database.read-only", ConfigValueType.OBJECT)
            .name("Read-Only Database Connection")
            .description("A connection to a secondary/read-only database, this connection will only be used for read operations.")
            .build();

    public static final ConfigType<String> PLUGINS_DIRECTORY = ConfigType.builder("server.plugins.directory", ConfigValueType.STRING)
            .name("Plugins Load Directory")
            .defaultValue("./plugins")
            .description("Defines the directory used to load plugins from.")
            .build();

    public static final ConfigType<Boolean> PLUGINS_SKIP_STARTUP_CHECK = ConfigType.builder("server.plugins.skip-startup-check", ConfigValueType.BOOL)
            .name("Disable Plugin Load Check")
            .defaultValue(false)
            .description("Disables the plugin startup check.")
            .build();

    public static final ConfigType<String> AUDIT_SPOOL_DIRECTORY = ConfigType.builder("server.audit-directory", ConfigValueType.STRING)
            .name("Audit Directory")
            .description("Local directory for audit spool and archive files")
            .defaultValue("./data/audit")
            .build();

    public static final ConfigType<String> WEB_DIRECTORY = ConfigType.builder("server.web-directory", ConfigValueType.STRING)
            .name("Web Client Directory")
            .description("Optional static SPA directory (e.g. ./static after unpacking orm-web-static-*.zip), leave empty when the UI is hosted separately (nginx/IIS/etc)")
            .defaultValue("./static")
            .build();

    // Database ony settings

    public static final ConfigType<String> WORKGROUP_NAME = ConfigType.builder("workgroup.name", ConfigValueType.STRING)
            .name("Workgroup Name")
            .description("The name of the workgroup")
            .build();

    public static final ConfigType<UUID> DEFAULT_FILE_STORE = ConfigType.builder("workgroup.default_file_store", ConfigValueType.UUID)
            .name("Default File Store")
            .description("Sets the default store used to store new files.")
            .build();

    // Either server or database settings

    public static final ConfigType<Boolean> DEBUG_DETAILED_ERRORS = ConfigType.builder("app.debug.detailed-errors", ConfigValueType.BOOL)
            .name("Return Detailed API Errors")
            .description("If enabled, the API will return detailed errors when they occur. This should only be enabled for debugging.")
            .defaultValue(false)
            .build();

    public static final ConfigType<Boolean> DEBUG_SHOW_SQL = ConfigType.builder("app.debug.show-sql", ConfigValueType.BOOL)
            .name("Print All SQL to the Log")
            .description("If enabled, the API will log all SQL queries to the local log file")
            .defaultValue(false)
            .build();

    public static final String DATABASE_PROBE_INTERVAL_MS_KEY = "app.database.probe-interval-ms";
    public static final ConfigType<Integer> DATABASE_PROBE_INTERVAL_MS = ConfigType.builder(DATABASE_PROBE_INTERVAL_MS_KEY, ConfigValueType.INT)
            .name("Database Health Probe Interval")
            .description("Defines how frequently the application probes the primary database to update its health status. This is used for the audit process to ensure the database is ready to accept new audit events")
            .defaultValue(30000)
            .build();

    public static final ConfigType<String[]> CORS_ALLOWED_ORIGINS = ConfigType.builder("app.security.cors.allowed-origins", ConfigValueType.STRING_LIST)
            .name("Cross Origin Resource Sharing Allowed Origins")
            .description("Origins that should be allowed to request resources from the API, this is your web client URL(s)")
            .defaultValue(new String[]{"http://localhost:5173", "http://localhost:3000"})
            .build();

    public static final ConfigType<String[]> CORS_ALLOWED_HEADERS = ConfigType.builder("app.security.cors.allowed-headers", ConfigValueType.STRING_LIST)
            .name("Cross Origin Resource Sharing Allowed Headers")
            .description("Headers that should be allowed to when accessing the API")
            .defaultValue(new String[]{"Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Client-Platform", "X-ORM-Audit-Comment"})
            .build();

    public static final ConfigType<Integer> TOKEN_EXPIRATION_TIME = ConfigType.builder("app.security.token-expiration-time", ConfigValueType.INT)
            .name("Token Expiration Time")
            .description("How long issued authentication tokens are valid for after issuing")
            .defaultValue(3600000)
            .build();

    public static final ConfigType<String> COOKIE_NAME = ConfigType.builder("app.security.cookie-auth.name", ConfigValueType.STRING)
            .name("Cookie Name")
            .description("When using cookie-based authentication, the name of the cookie to use")
            .defaultValue("ORM-Authentication")
            .build();

    public static final ConfigType<Boolean> COOKIE_SECURE = ConfigType.builder("app.security.cookie-auth.secure", ConfigValueType.BOOL)
            .name("Cookie Secure Only")
            .description("When using cookie-based authentication, enforce the 'Secure' attribute")
            .defaultValue(true)
            .build();

    // Web UI branding

    public static final ConfigType<String> WEB_PRODUCT_NAME = ConfigType.builder("app.web.product-name", ConfigValueType.STRING)
            .name("Product Name")
            .description("Product name shown in the web client.")
            .defaultValue("Open Records Manager")
            .build();

    public static final ConfigType<String> WEB_LOGO_URL = ConfigType.builder("app.web.logo-url", ConfigValueType.STRING)
            .name("Logo URL")
            .description("Optional logo image URL for the web client.")
            .defaultValue("")
            .build();

    public static final ConfigType<String> WEB_FAVICON_URL = ConfigType.builder("app.web.favicon-url", ConfigValueType.STRING)
            .name("Favicon URL")
            .description("Optional favicon URL for the web client.")
            .defaultValue("/favicon.ico")
            .build();

    public static final ConfigType<String> WEB_PRIMARY_COLOR = ConfigType.builder("app.web.primary-color", ConfigValueType.STRING)
            .name("Primary Color")
            .description("Primary brand color (CSS) for the web client.")
            .defaultValue("#1d4ed8")
            .build();

    public static final ConfigType<String> WEB_SUPPORT_URL = ConfigType.builder("app.web.support-url", ConfigValueType.STRING)
            .name("Support URL")
            .description("Optional support link shown in the web client.")
            .defaultValue("")
            .build();

    // Audit settings

    public static final ConfigType<Boolean> AUDIT_ENABLED = ConfigType.builder("app.audit.enabled", ConfigValueType.BOOL)
            .name("Audit Enabled")
            .description("Master switch for the audit system")
            .defaultValue(true)
            .build();

    public static final ConfigType<Integer> AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS = ConfigType.builder(
                    "app.audit.spool-drain-interval-seconds",
                    ConfigValueType.INT
            )
            .name("Audit Spool Drain Interval")
            .description("Seconds between background attempts to drain the audit spool into the database.")
            .defaultValue(30)
            .build();

    public static final ConfigType<Boolean> AUDIT_FILE_ARCHIVE_ENABLED = ConfigType.builder(
                    "app.audit.archive-enabled",
                    ConfigValueType.BOOL
            )
            .name("Audit Archive Log Enabled")
            .description("When enabled, synced audit events are also appended to a daily rotating archive log file.")
            .defaultValue(true)
            .build();

}
