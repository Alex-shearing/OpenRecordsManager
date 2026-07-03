package com.openrecordsmanager.api;

import com.openrecordsmanager.api.config.ConfigDefinition;

public interface PluginContext {
    void registerComponent(String id, Component component);

    default void registerConfig(ConfigDefinition<?> config) {
        this.registerComponent(config.key(), config);
    }
}
