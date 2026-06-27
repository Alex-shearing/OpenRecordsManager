package com.openrecordsmanager.config;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.types.ComponentTypes;
import jakarta.annotation.Nullable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConfigStoreImpl extends EnumerablePropertySource<ConfigStoreImpl> implements ConfigStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigStoreImpl.class);

    private final String[] stringKeys;
    private final Map<ConfigDefinition<?>, ConfigStoreImpl.ConfigValue<?>> configs;
    @Nullable
    private final DataRepository repository;

    public ConfigStoreImpl(@Nullable ComponentCatalog pluginManager, @Nullable DataRepository repository) {
        super("custom_config");
        this.repository = repository;

        Collection<ConfigDefinition<?>> pluginConfigs = List.of();
        if (pluginManager != null) {
            pluginConfigs = pluginManager.getComponents(ComponentTypes.CONFIG);
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
            String envVal = System.getenv(configProperty.getEnvName());
            if (envVal != null) {
                LOGGER.info("Loading config from environment variable {}", configProperty.getEnvName());
                if (!configValue.setValueFromString(envVal)) {
                    LOGGER.warn("Failed to parse config from environment variable {}", configProperty.getEnvName());
                }
            }

            // Load from database variables
            if (repository != null) {
                repository.configRepo.findByConfigKey(configProperty.id()).ifPresent(dbConfig -> {
                    LOGGER.info("Loading config from database value {}", dbConfig.configKey);
                    if (dbConfig.configValue != null && !dbConfig.configValue.isBlank()) {
                        if (!configValue.setValueFromString(dbConfig.configValue)) {
                            LOGGER.warn("Failed to parse config from database value {}", dbConfig.configKey);
                        }
                    }
                });
            }
        });

        this.configs = properties;
        Set<String> keys = new HashSet<>();
        for (ConfigDefinition<?> property : this.configs.keySet()) {
            keys.add(property.id());
            if (property.alias() != null) {
                keys.add(property.alias());
            }
        }
        this.stringKeys = keys.toArray(String[]::new);
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(ConfigDefinition<T> key) {
        if (this.repository != null) {
            this.repository.configRepo.findByConfigKey(key.id())
                    .ifPresent(dbConfig -> this.configs.get(key).setValueFromString(dbConfig.configValue));
        }
        return (T) this.configs.get(key).value;
    }

    @Override
    public Set<ConfigDefinition<?>> getProperties() {
        return this.configs.keySet();
    }

    @Override
    @NullMarked
    public String[] getPropertyNames() {
        return this.stringKeys;
    }

    @Override
    public @Nullable Object getProperty(@NonNull String name) {
        return this.configs.keySet().stream()
                .filter(configProperty -> configProperty.isKey(name))
                .map(configProperty -> this.configs.get(configProperty).value)
                .findFirst()
                .orElse(null);
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
