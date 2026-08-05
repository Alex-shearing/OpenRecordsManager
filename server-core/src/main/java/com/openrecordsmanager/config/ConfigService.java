package com.openrecordsmanager.config;

import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.errors.ResourceNotFoundException;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.config.dto.ConfigResponse;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConfigService implements ConfigStore {

    private final Environment environment;
    private final DataRepository repository;
    private final ComponentCatalog catalog;

    public ConfigService(Environment environment, DataRepository repository, ComponentCatalog catalog) {
        this.environment = environment;
        this.repository = repository;
        this.catalog = catalog;
    }

    @Transactional
    public ConfigResponse setConfig(String id, String value) {
        ConfigType<?> key = this.getConfigByKey(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.CONFIG.name, id));

        Object parsedValue = key.type().fromString(value)
                .orElseThrow(() -> new IllegalArgumentException(MessageFormat.format(
                        "Unable to parse configuration value as {0}",
                        key.type().toString()
                )));

        ConfigItem config = this.repository.configRepo.findByConfigKey(id)
                .orElseGet(() -> new ConfigItem(id, value));
        config.configValue = value;

        this.repository.configRepo.saveAndFlush(config);

        return new ConfigResponse(config.configKey, parsedValue);
    }

    @Override
    public <T> @Nullable T getValue(ConfigType<T> key) {
        T value = this.environment.getProperty(key.key(), key.type().cType);
        return value != null ? value : key.defaultValue();
    }

    public Optional<ConfigType<?>> getConfigByKey(String key) {
        return this.catalog.getRegistry(ComponentTypes.CONFIG).stream()
                .filter(configDefinition -> configDefinition.key().equals(key))
                .findFirst();
    }

    public Map<String, Optional<?>> getAllConfig() {
        return this.catalog.getRegistry(ComponentTypes.CONFIG).stream()
                .map(def -> Map.entry(def.key(), this.getOptional(def)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Optional<?> getDatabaseConfig(String id) {
        ConfigType<?> config = this.getConfigByKey(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.CONFIG.name, id));

        ConfigItem configItem = this.repository.configRepo.findByConfigKey(config.key())
                .orElseThrow(() -> new ResourceNotFoundException("config value", id));

        return config.type().fromString(configItem.configValue);
    }

}
