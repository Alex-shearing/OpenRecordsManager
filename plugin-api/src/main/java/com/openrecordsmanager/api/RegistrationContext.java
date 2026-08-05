package com.openrecordsmanager.api;

import com.openrecordsmanager.api.config.ConfigType;

public interface RegistrationContext {
    void registerComponent(String id, Component component);

    default void registerConfig(ConfigType<?>... configs) {
        for (ConfigType<?> config : configs) {
            this.registerComponent(config.key(), config);
        }
    }
}
