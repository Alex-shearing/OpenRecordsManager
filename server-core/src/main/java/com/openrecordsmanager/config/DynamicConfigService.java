package com.openrecordsmanager.config;

import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.ComponentCatalog;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DynamicConfigService implements ConfigStore {

    private final Environment environment;

    public DynamicConfigService(Environment environment) {
        this.environment = environment;
    }

    @Override
    public <T> @Nullable T getValue(ConfigDefinition<T> key) {
        T value = this.environment.getProperty(key.key(), key.type().cType);
        return value != null ? value : key.defaultValue();
    }

    public Optional<ConfigDefinition<?>> getConfigByKey(String key, ComponentCatalog catalog) {
        return catalog.getComponents(ComponentTypes.CONFIG).stream()
                .filter(configDefinition -> configDefinition.key().equals(key))
                .findFirst();
    }

}
