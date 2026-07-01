package com.openrecordsmanager.config;

import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.config.ConfigValueType;

import java.util.UUID;

public class ConfigProperties {
    public static final ConfigDefinition<String> WORKGROUP_DATABASE_URL = ConfigDefinition.builder("workgroup.database", ConfigValueType.STRING)
            .name("Database URL")
            .description("A JDBC URL to the database")
            .build();

    public static final ConfigDefinition<String> WORKGROUP_NAME = ConfigDefinition.builder("workgroup.name", ConfigValueType.STRING)
            .name("Workgroup Name")
            .description("The name of the workgroup")
            .build();

    public static final ConfigDefinition<UUID> WORKGROUP_DEFAULT_FILE_STORE = ConfigDefinition.builder("workgroup.default_file_store", ConfigValueType.UUID)
            .name("Default File Store")
            .description("Sets the default store used to store new files.")
            .build();

    public static final ConfigDefinition<Boolean> DETAILED_ERRORS = ConfigDefinition.builder("server.debug.detailed_errors", ConfigValueType.BOOL)
            .name("Return Detailed API Errors")
            .description("If enabled, the API will return detailed errors when they occur. This should only be enabled for debugging.")
            .defaultValue(false)
            .build();

    public static final ConfigDefinition<String> PLUGIN_LOAD_DIRECTORY = ConfigDefinition.builder("server.plugins.directory", ConfigValueType.STRING)
            .name("Plugins Load Directory")
            .defaultValue("./plugins")
            .description("Defines the directory used to load plugins from.")
            .build();

    public static final ConfigDefinition<Boolean> SKIP_PLUGIN_LOAD_CHECK = ConfigDefinition.builder("server.plugins.skip_startup_check", ConfigValueType.BOOL)
            .name("Disable Plugin Load Check")
            .defaultValue(false)
            .description("Disables the plugin startup check.")
            .build();

    public static final ConfigDefinition<?>[] BUILTIN_CONFIG = {
            WORKGROUP_DATABASE_URL, WORKGROUP_NAME, WORKGROUP_DEFAULT_FILE_STORE,
            DETAILED_ERRORS,
            PLUGIN_LOAD_DIRECTORY, SKIP_PLUGIN_LOAD_CHECK
    };
}
