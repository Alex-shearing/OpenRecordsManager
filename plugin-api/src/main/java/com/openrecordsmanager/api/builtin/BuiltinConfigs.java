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

    public static final ConfigType<String> DATABASE_PRIMARY_URL = ConfigType.builder("server.database.primary.url", ConfigValueType.STRING)
            .name("Primary Database URL")
            .description("A JDBC URL to the connect to the database, this connection will be used for read/write operations.")
            .build();

    public static final ConfigType<Object> DATABASE_READ_ONLY = ConfigType.builder("server.database.read-only", ConfigValueType.OBJECT)
            .name("Read-Only Database Connection")
            .description("A connection to a secondary/read-only database, this connection will only be used for read operations.")
            .build();

    public static final ConfigType<String> DATABASE_READ_ONLY_URL = ConfigType.builder("server.database.read-only.url", ConfigValueType.STRING)
            .name("Read-Only Database URL")
            .description("A JDBC URL to the connect to a replica of the database, this connection will only be used for read operations.")
            .build();


    public static final ConfigType<String> PLUGINS_DIRECTORY = ConfigType.builder("server.plugins.directory", ConfigValueType.STRING)
            .name("Plugins Load Directory")
            .defaultValue("./plugins")
            .description("Defines the directory used to load plugins from.")
            .build();

    public static final ConfigType<Boolean> PLUGINS_SKIP_STARTUP_CHECK = ConfigType.builder("server.plugins.skip_startup_check", ConfigValueType.BOOL)
            .name("Disable Plugin Load Check")
            .defaultValue(false)
            .description("Disables the plugin startup check.")
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

    public static final ConfigType<Boolean> DEBUG_DETAILED_ERRORS = ConfigType.builder("app.debug.detailed_errors", ConfigValueType.BOOL)
            .name("Return Detailed API Errors")
            .description("If enabled, the API will return detailed errors when they occur. This should only be enabled for debugging.")
            .defaultValue(false)
            .build();

    // Web UI branding

    public static final ConfigType<String> WEB_PRODUCT_NAME = ConfigType.builder("app.web.product_name", ConfigValueType.STRING)
            .name("Product Name")
            .description("Product name shown in the web client.")
            .defaultValue("Open Records Manager")
            .build();

    public static final ConfigType<String> WEB_LOGO_URL = ConfigType.builder("app.web.logo_url", ConfigValueType.STRING)
            .name("Logo URL")
            .description("Optional logo image URL for the web client.")
            .defaultValue("")
            .build();

    public static final ConfigType<String> WEB_FAVICON_URL = ConfigType.builder("app.web.favicon_url", ConfigValueType.STRING)
            .name("Favicon URL")
            .description("Optional favicon URL for the web client.")
            .defaultValue("/favicon.ico")
            .build();

    public static final ConfigType<String> WEB_PRIMARY_COLOR = ConfigType.builder("app.web.primary_color", ConfigValueType.STRING)
            .name("Primary Color")
            .description("Primary brand color (CSS) for the web client.")
            .defaultValue("#1d4ed8")
            .build();

    public static final ConfigType<String> WEB_SUPPORT_URL = ConfigType.builder("app.web.support_url", ConfigValueType.STRING)
            .name("Support URL")
            .description("Optional support link shown in the web client.")
            .defaultValue("")
            .build();

    // Audit settings

    public static final ConfigType<Boolean> AUDIT_ENABLED = ConfigType.builder("audit.enabled", ConfigValueType.BOOL)
            .name("Audit Enabled")
            .description("Master switch for the audit system.")
            .defaultValue(true)
            .build();

    public static final ConfigType<String> AUDIT_SPOOL_DIRECTORY = ConfigType.builder("audit.spool.directory", ConfigValueType.STRING)
            .name("Audit Spool Directory")
            .description("Local directory for audit spool and archive log files.")
            .defaultValue("./data/audit")
            .build();

    public static final ConfigType<Integer> AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS = ConfigType.builder(
                    "audit.spool.drain_interval_seconds",
                    ConfigValueType.INT
            )
            .name("Audit Spool Drain Interval")
            .description("Seconds between background attempts to drain the audit spool into the database.")
            .defaultValue(30)
            .build();

    public static final ConfigType<Boolean> AUDIT_FILE_ARCHIVE_ENABLED = ConfigType.builder(
                    "audit.file.archive_enabled",
                    ConfigValueType.BOOL
            )
            .name("Audit Archive Log Enabled")
            .description("When enabled, synced audit events are also appended to a daily rotating archive log file.")
            .defaultValue(true)
            .build();

}
