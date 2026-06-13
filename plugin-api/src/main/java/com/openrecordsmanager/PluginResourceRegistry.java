package com.openrecordsmanager;

import com.openrecordsmanager.config.ConfigProperty;

public interface PluginResourceRegistry {
    void registerConfig(ConfigProperty<?> property);

    void registerInstanceComponents(RegisterableComponent... types);
}
