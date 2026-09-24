package com.openrecordsmanager.config;

import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.text.MessageFormat;
import java.time.Instant;

@Entity
@Table(name = "system_configurations")
public class ConfigItem {

    @Id
    @Column(name = "config_key", unique = true, nullable = false)
    private String configKey;

    @Column(name = "config_value")
    @JdbcTypeCode(SqlTypes.JSON)
    @Nullable
    private JsonNode configValue;

    @Column(nullable = false)
    private Instant dateModified;

    @Deprecated
    protected ConfigItem() {
    }

    public ConfigItem(ComponentCatalog catalog, String configKey, @Nullable JsonNode configValue) {
        this.configKey = configKey;
        this.dateModified = Instant.now();
        this.setValue(catalog, configValue);
    }

    public String getKey() {
        return this.configKey;
    }

    public ConfigType<?> getConfigKey(ComponentCatalog catalog) {
        return catalog.getRegistry(ComponentTypes.CONFIG).stream()
                .filter(configDefinition -> configDefinition.key().equals(this.configKey))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.CONFIG, this.configKey));
    }

    public @Nullable JsonNode getValue() {
        return this.configValue;
    }

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
    }

    public void setValue(ComponentCatalog catalog, @Nullable JsonNode value) {
        ConfigType<?> key = this.getConfigKey(catalog);
        Object parsed = key.type().parse(value);
        if (parsed == null && value != null && !value.isNull()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Unable to parse configuration value as {0}",
                    key.type().getName()
            ));
        }
        this.configValue = value;
        this.touchDateModified();
    }
}
