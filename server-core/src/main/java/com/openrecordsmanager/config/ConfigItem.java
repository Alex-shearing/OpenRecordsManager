package com.openrecordsmanager.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    public ConfigItem(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

}
