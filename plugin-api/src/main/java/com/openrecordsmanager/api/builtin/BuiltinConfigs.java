package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.config.ConfigValueType;

import java.util.UUID;

public class BuiltinConfigs {

    // Server only settings

    public static final ConfigType<Object> DATABASE_PRIMARY = ConfigType.builder("server.database.primary", ConfigValueType.OBJECT)
            .name("Primary Database")
            .description("The primary connection to the database, this connection will be used for read/write operations.")
            .build();

    public static final ConfigType<String> DATABASE_PRIMARY_URL = ConfigType.builder("server.database.primary.url", ConfigValueType.STRING)
            .name("Primary Database URL")
            .description("A JDBC URL to the connect to the database, this connection will be used for read/write operations.")
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

}
