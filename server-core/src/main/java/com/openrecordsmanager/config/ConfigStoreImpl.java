package com.openrecordsmanager.config;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.resources.ResourceRegistry;
import com.openrecordsmanager.resources.ResourceType;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.*;

public class ConfigStoreImpl extends EnumerablePropertySource<ConfigStoreImpl> implements ConfigStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigStoreImpl.class);

    private final String[] stringKeys;
    private final Map<ConfigDefinition<?>, ConfigStoreImpl.ConfigValue<?>> configs;

    private ConfigStoreImpl(Map<ConfigDefinition<?>, ConfigValue<?>> configs) {
        super("custom_config");
        this.configs = configs;
        Set<String> keys = new HashSet<>();
        for (ConfigDefinition<?> property : this.configs.keySet()) {
            keys.add(property.id());
            if (property.alias() != null) {
                keys.add(property.alias());
            }
        }
        this.stringKeys = keys.toArray(String[]::new);
    }

    public Set<ConfigDefinition<?>> getProperties() {
        return this.configs.keySet();
    }

    public <T> T getProperty(ConfigDefinition<T> key) {
        return (T) this.configs.get(key).value;
    }

    @Override
    public String[] getPropertyNames() {
        return this.stringKeys;
    }

    @Override
    public @org.jspecify.annotations.Nullable Object getProperty(String name) {
        return this.configs.keySet().stream().filter(configProperty -> configProperty.isKey(name))
                .map(configProperty -> this.configs.get(configProperty).value).findFirst().orElse(null);
    }

    public static ConfigStoreImpl build(@Nullable ResourceRegistry pluginManager) {
        Collection<ConfigDefinition<?>> pluginConfigs = List.of();
        if (pluginManager != null) {
            pluginConfigs = pluginManager.getComponents(ResourceType.CONFIG);
        }

        int totalConfig = ConfigProperties.BUILTIN_CONFIG.length + pluginConfigs.size();
        ImmutableMap.Builder<ConfigDefinition<?>, ConfigStoreImpl.ConfigValue<?>> propertiesBuilder = ImmutableMap.builderWithExpectedSize(totalConfig);

        // Load defaults into map
        for (ConfigDefinition<?> property : ConfigProperties.BUILTIN_CONFIG) {
            propertiesBuilder.put(property, new ConfigStoreImpl.ConfigValue<>(property));
        }
        for (ConfigDefinition<?> property : pluginConfigs) {
            propertiesBuilder.put(property, new ConfigStoreImpl.ConfigValue<>(property));
        }

        Map<ConfigDefinition<?>, ConfigValue<?>> properties = propertiesBuilder.build();

        // Load from ENV variables
        properties.forEach((configProperty, configValue) -> {
            String var = System.getenv(configProperty.getEnvName());
            if (var != null) {
                LOGGER.info("Loading config from environment variable {}", configProperty.getEnvName());
                if (!configValue.setValueFromString(var)) {
                    LOGGER.warn("Failed to parse config from environment variable {}", configProperty.getEnvName());
                }
            }
        });

        return new ConfigStoreImpl(properties);
    }


    public static class ConfigValue<T> {
        private final ConfigDefinition<T> key;
        private T value;

        public ConfigValue(ConfigDefinition<T> key) {
            this.key = key;
            this.value = key.defaultValue();
        }

        public void setValue(T value) {
            this.value = value;
        }

        public boolean setValueFromString(String value) {
            Optional<T> newVal = this.key.type().fromString(value);
            if (newVal.isPresent()) {
                this.setValue(newVal.get());
                return true;
            }

            return false;
        }
    }
}
