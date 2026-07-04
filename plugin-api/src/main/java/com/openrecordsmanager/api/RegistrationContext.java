package com.openrecordsmanager.api;

import com.openrecordsmanager.api.config.ConfigDefinition;

public interface RegistrationContext {
    void registerComponent(String id, Component component);

    default void registerConfig(ConfigDefinition<?>... configs) {
        for (ConfigDefinition<?> config : configs) {
            this.registerComponent(config.key(), config);
        }
    }
}
