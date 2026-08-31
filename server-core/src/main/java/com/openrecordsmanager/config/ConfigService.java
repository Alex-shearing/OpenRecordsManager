package com.openrecordsmanager.config;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditContext;
import com.openrecordsmanager.audit.AuditEventDescriptions;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.config.dto.ConfigResponse;
import com.openrecordsmanager.config.dto.ConfigTypeResponse;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfigService implements ConfigStore {

    private final Environment environment;
    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final AuditService auditService;
    private final AuditPolicyService auditPolicyService;

    public ConfigService(
            Environment environment,
            DataRepository repository,
            ComponentCatalog catalog,
            @Lazy AuditService auditService,
            @Lazy AuditPolicyService auditPolicyService
    ) {
        this.environment = environment;
        this.repository = repository;
        this.catalog = catalog;
        this.auditService = auditService;
        this.auditPolicyService = auditPolicyService;
    }

    @Transactional
    public ConfigResponse setConfig(String id, String value) {
        if (AuditContext.isCaptureEnabled()) {
            this.auditPolicyService.validateCommentRequired(AuditEntityType.CONFIG, AuditOperation.UPDATE);
        }
        ConfigItem oldConfig = this.repository.configRepo.findByConfigKey(id).orElse(null);
        String oldValue = oldConfig == null ? null : oldConfig.configValue;

        ConfigItem configItem = oldConfig != null ? oldConfig : new ConfigItem(this.catalog, id, value);
        configItem.setValue(catalog, value);

        this.repository.configRepo.saveAndFlush(configItem);

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.CONFIG,
                id,
                AuditEventDescriptions.singleChange("configValue", oldValue, value),
                null,
                null
        );

        return ConfigResponse.of(configItem);
    }

    @Override
    public <T> @Nullable T getValue(ConfigType<T> key) {
        T value = this.environment.getProperty(key.key(), key.type().cType);
        System.out.println(value);
        return value != null ? value : key.defaultValue();
    }

    private Optional<ConfigType<?>> getConfigByKey(String key) {
        return this.catalog.getRegistry(ComponentTypes.CONFIG).stream()
                .filter(configDefinition -> configDefinition.key().equals(key))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public <T> Set<ConfigTypeResponse> getAllConfigTypes() {
        Set<ConfigTypeResponse> results = this.catalog.getRegistry(ComponentTypes.CONFIG).stream()
                .filter(configType -> !configType.key().startsWith("server."))
                .map(cfg -> {
                    ConfigType<T> cfgT = (ConfigType<T>) cfg;

                    Optional<T> db = this.repository.configRepo.findByConfigKey(cfg.key())
                            .map(configItem -> cfgT.type().fromString(configItem.configValue))
                            .orElse(Optional.ofNullable(cfgT.defaultValue()));
                    return ConfigTypeResponse.from(cfgT, db.orElse(null));
                })
                .collect(Collectors.toSet());
        this.auditService.recordCollectionRead(AuditEntityType.CONFIG, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public Optional<?> getDatabaseConfig(String id) {
        ConfigType<?> config = this.getConfigByKey(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.CONFIG.name, id));

        this.repository.configRepo.findByConfigKey(config.key())
                .orElseThrow(() -> new ResourceNotFoundException("config value", id));

        this.auditService.addReadEvent(AuditEntityType.CONFIG, config.key());
        return this.getOptional(config);
    }

    @Transactional(readOnly = true)
    public Map<String, ?> getServerConfig() {
        Map<String, ?> results = this.catalog.getRegistry(ComponentTypes.CONFIG).stream()
                .map(config -> {
                    Object value = this.getValue(config);
                    if (value == null) return null;
                    return Map.entry(config.key(), value);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.auditService.recordCollectionRead(AuditEntityType.CONFIG, results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public Object getServerConfig(String id) {
        ConfigType<?> config = this.getConfigByKey(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.CONFIG.name, id));

        Object value = this.getOptional(config)
                .orElseThrow(() -> new ResourceNotFoundException("config value", id));

        this.auditService.addReadEvent(AuditEntityType.CONFIG, config.key());
        return value;
    }

}
