package com.openrecordsmanager.config;

import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.errors.ApiError;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.config.dto.ConfigResponse;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ComponentCatalog;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        ConfigDefinition<?> key = this.getConfigByKey(id)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.CONFIG.name, id));
        Object parsedValue = key.type().fromString(value)
                .orElseThrow(() -> ApiError.clientError(
                        "Unable to parse configuration value as {0}",
                        key.type().toString()
                ));

        ConfigItem config = this.repository.configRepo.findByConfigKey(id)
                .orElseGet(() -> new ConfigItem(id, value));
        config.configValue = value;

        this.repository.configRepo.saveAndFlush(config);

        return new ConfigResponse(config.configKey, parsedValue);
    }

    @Override
    public <T> @Nullable T getValue(ConfigDefinition<T> key) {
        T value = this.environment.getProperty(key.key(), key.type().cType);
        return value != null ? value : key.defaultValue();
    }

    public Optional<ConfigDefinition<?>> getConfigByKey(String key) {
        return catalog.getComponents(ComponentTypes.CONFIG).stream()
                .filter(configDefinition -> configDefinition.key().equals(key))
                .findFirst();
    }

    public Map<String, Optional<?>> getAllConfig() {
        return this.catalog.getComponents(ComponentTypes.CONFIG).stream()
                .map(def -> Map.entry(def.key(), this.getOptional(def)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
