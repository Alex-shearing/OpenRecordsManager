package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.config.ConfigValueType;

import java.util.UUID;

public class BuiltinConfigs {

    // Server only settings

    public static final ConfigDefinition<Object> DATABASE_PRIMARY = ConfigDefinition.builder("server.database.primary", ConfigValueType.OBJECT)
            .name("Primary Database")
            .description("The primary connection to the database, this connection will be used for read/write operations.")
            .build();

    public static final ConfigDefinition<String> DATABASE_PRIMARY_URL = ConfigDefinition.builder("server.database.primary.url", ConfigValueType.STRING)
            .name("Primary Database URL")
            .description("A JDBC URL to the connect to the database, this connection will be used for read/write operations.")
            .build();

    public static final ConfigDefinition<String> DATABASE_READ_ONLY_URL = ConfigDefinition.builder("server.database.read-only.url", ConfigValueType.STRING)
            .name("Read-Only Database URL")
            .description("A JDBC URL to the connect to a replica of the database, this connection will only be used for read operations.")
            .build();


    public static final ConfigDefinition<String> PLUGINS_DIRECTORY = ConfigDefinition.builder("server.plugins.directory", ConfigValueType.STRING)
            .name("Plugins Load Directory")
            .defaultValue("./plugins")
            .description("Defines the directory used to load plugins from.")
            .build();

    public static final ConfigDefinition<Boolean> PLUGINS_SKIP_STARTUP_CHECK = ConfigDefinition.builder("server.plugins.skip_startup_check", ConfigValueType.BOOL)
            .name("Disable Plugin Load Check")
            .defaultValue(false)
            .description("Disables the plugin startup check.")
            .build();

    // Database ony settings

    public static final ConfigDefinition<String> WORKGROUP_NAME = ConfigDefinition.builder("workgroup.name", ConfigValueType.STRING)
            .name("Workgroup Name")
            .description("The name of the workgroup")
            .build();

    public static final ConfigDefinition<UUID> DEFAULT_FILE_STORE = ConfigDefinition.builder("workgroup.default_file_store", ConfigValueType.UUID)
            .name("Default File Store")
            .description("Sets the default store used to store new files.")
            .build();

    // Either server or database settings

    public static final ConfigDefinition<Boolean> DEBUG_DETAILED_ERRORS = ConfigDefinition.builder("app.debug.detailed_errors", ConfigValueType.BOOL)
            .name("Return Detailed API Errors")
            .description("If enabled, the API will return detailed errors when they occur. This should only be enabled for debugging.")
            .defaultValue(false)
            .build();

}
