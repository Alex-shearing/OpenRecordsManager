package com.openrecordsmanager.config;

import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.config.ConfigValueType;

public class ConfigProperties {
    public static final ConfigDefinition<String> WORKGROUP_DATABASE_URL = ConfigDefinition.builder("workgroup.database", ConfigValueType.STRING)
            .name("Database URL")
            .alias("spring.datasource.url")
            .description("A JDBC URL to the database")
            .build();

    public static final ConfigDefinition<String> WORKGROUP_NAME = ConfigDefinition.builder("workgroup.name", ConfigValueType.STRING)
            .name("Workgroup Name")
            .description("The name of the workgroup")
            .defaultValue("workgroup")
            .build();

    public static final ConfigDefinition<String> LOGGING_LEVEL = ConfigDefinition.builder("logging.level", ConfigValueType.STRING)
            .name("Workgroup Log Level")
            .alias("logging.level.root")
            .description("The log level of the workgroup server")
            .defaultValue("INFO")
            .build();

    public static final ConfigDefinition<?>[] BUILTIN_CONFIG = {WORKGROUP_DATABASE_URL, WORKGROUP_NAME, LOGGING_LEVEL};
}
