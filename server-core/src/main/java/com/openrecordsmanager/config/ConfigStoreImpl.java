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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class ConfigStoreImpl extends EnumerablePropertySource<ConfigStoreImpl> implements ConfigStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigStoreImpl.class);

    private final String[] stringKeys;
    private final Map<ConfigDefinition<?>, ConfigStoreImpl.ConfigValue<?>> configs;
    @Nullable
    private final DataRepository repository;

    public ConfigStoreImpl(@Nullable ComponentCatalog catalog, @Nullable DataRepository repository) {
        super("custom_config");
        this.repository = repository;

        ImmutableMap.Builder<ConfigDefinition<?>, ConfigStoreImpl.ConfigValue<?>> propertiesBuilder = ImmutableMap.builder();

        // Load defaults into map
        for (ConfigDefinition<?> property : ConfigProperties.BUILTIN_CONFIG) {
            propertiesBuilder.put(property, new ConfigStoreImpl.ConfigValue<>(property));
        }
        if (catalog != null) {
            for (ConfigDefinition<?> property : catalog.getComponents(ComponentTypes.CONFIG)) {
                propertiesBuilder.put(property, new ConfigStoreImpl.ConfigValue<>(property));
            }
        }

        Map<ConfigDefinition<?>, ConfigValue<?>> configs = propertiesBuilder.build();

        // Load in defaults
        configs.forEach((configProperty, configValue) -> {
            // Load from ENV
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

        this.configs = configs;
        this.stringKeys = this.configs.keySet().stream()
                .flatMap(def -> Stream.of(def.id(), def.alias()))
                .filter(Objects::nonNull)
                .toArray(String[]::new);
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
