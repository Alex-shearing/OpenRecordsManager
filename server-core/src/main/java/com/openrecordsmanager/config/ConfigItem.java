package com.openrecordsmanager.config;

import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.text.MessageFormat;

@Entity
@Table(name = "system_configurations")
public class ConfigItem {

    @Id
    @Column(name = "config_key", unique = true, nullable = false)
    public String configKey;

    @Column(name = "config_value")
    public String configValue;

    @Deprecated
    protected ConfigItem() {
    }

    public ConfigItem(ComponentCatalog catalog, String configKey, String configValue) {
        this.configKey = configKey;
        this.setValue(catalog, configValue);
    }

    public ConfigType<?> getConfigKey(ComponentCatalog catalog) {
        return catalog.getRegistry(ComponentTypes.CONFIG).stream()
                .filter(configDefinition -> configDefinition.key().equals(this.configKey))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.CONFIG, this.configKey));
    }

    public Object getValue(ComponentCatalog catalog) {
        ConfigType<?> key = this.getConfigKey(catalog);
        return key.type().fromString(this.configValue)
                .orElseThrow(() -> new IllegalArgumentException(MessageFormat.format(
                        "Unable to parse configuration value as {0}",
                        key.type().toString()
                )));
    }

    public void setValue(ComponentCatalog catalog, String value) {
        ConfigType<?> key = this.getConfigKey(catalog);
        key.type().fromString(value)
                .orElseThrow(() -> new IllegalArgumentException(MessageFormat.format(
                        "Unable to parse configuration value as {0}",
                        key.type().toString()
                )));
        this.configValue = value;
    }
}
