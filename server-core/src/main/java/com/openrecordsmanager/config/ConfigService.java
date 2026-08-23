package com.openrecordsmanager.config;

import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.config.dto.ConfigResponse;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
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
        ConfigItem config = this.repository.configRepo.findByConfigKey(id)
                .orElseGet(() -> new ConfigItem(this.catalog, id, value));
        config.setValue(catalog, value);

        this.repository.configRepo.saveAndFlush(config);

        return ConfigResponse.of(config);
    }

    @Override
    public <T> @Nullable T getValue(ConfigType<T> key) {
        T value = this.environment.getProperty(key.key(), key.type().cType);
        return value != null ? value : key.defaultValue();
    }

    private Optional<ConfigType<?>> getConfigByKey(String key) {
        return this.catalog.getRegistry(ComponentTypes.CONFIG).stream()
                .filter(configDefinition -> configDefinition.key().equals(key))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Map<String, Optional<?>> getAllConfig() {
        return this.repository.configRepo.findAll().stream()
                .map(def -> Map.entry(def.configKey, this.getOptional(def.getConfigKey(this.catalog))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Transactional(readOnly = true)
    public Optional<?> getDatabaseConfig(String id) {
        ConfigType<?> config = this.getConfigByKey(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.CONFIG.name, id));

        ConfigItem configItem = this.repository.configRepo.findByConfigKey(config.key())
                .orElseThrow(() -> new ResourceNotFoundException("config value", id));

        return config.type().fromString(configItem.configValue);
    }

    @Transactional(readOnly = true)
    public Map<String, ?> getServerConfig() {
        return this.catalog.getRegistry(ComponentTypes.CONFIG).stream()
                .map(config -> {
                    Object value = this.getValue(config);
                    if (value == null) return null;
                    return Map.entry(config.key(), value);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Transactional(readOnly = true)
    public Object getServerConfig(String id) {
        ConfigType<?> config = this.getConfigByKey(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.CONFIG.name, id));

        return this.getOptional(config)
                .orElseThrow(() -> new ResourceNotFoundException("config value", id));
    }

}
