package com.openrecordsmanager.config;

public class ConfigProperties {
    public static final ConfigDefinition<String> WORKGROUP_DATABASE_URL = ConfigDefinition.builder("workgroup.database", ConfigValueType.STRING)
            .name("Database URL")
            .alias("spring.datasource.url")
            .description("A JDBC URL to the database")
            .defaultValue("asdasd")
            .build();

    public static final ConfigDefinition<String> WORKGROUP_NAME = ConfigDefinition.builder("workgroup.name", ConfigValueType.STRING)
            .name("Workgroup Name")
            .description("The name of the workgroup")
            .defaultValue("workgroup")
            .build();

    public static ConfigDefinition<?>[] BUILTIN_CONFIG = {WORKGROUP_DATABASE_URL, WORKGROUP_NAME};
}
