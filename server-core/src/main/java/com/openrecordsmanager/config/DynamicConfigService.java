package com.openrecordsmanager.config;

import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.config.ConfigStore;
import org.jspecify.annotations.NonNull;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class DynamicConfigService implements ConfigStore {

    private final Environment environment;

    public DynamicConfigService(Environment environment) {
        this.environment = environment;
    }

    @Override
    public <T> T getProperty(@NonNull ConfigDefinition<T> key) {
        T value = this.environment.getProperty(key.key(), key.type().cType);
        return value != null ? value : key.defaultValue();
    }

}
