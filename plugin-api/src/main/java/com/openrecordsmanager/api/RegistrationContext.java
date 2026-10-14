package com.openrecordsmanager.api;

import com.openrecordsmanager.api.config.ConfigType;

public interface RegistrationContext {
    /**
     * Plugin identity from {@code plugin.json}
     */
    String id();

    /**
     * Register a component for this plugin
     */
    <T extends Component> ComponentReference<T> registerComponent(String id, T component);

    /**
     * Register a configuration component for this plugin
     */
    default void registerConfig(ConfigType<?>... configs) {
        for (ConfigType<?> config : configs) {
            this.registerComponent(config.key(), config);
        }
    }
}
