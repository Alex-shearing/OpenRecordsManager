package com.openrecordsmanager.model;

import jakarta.persistence.*;

@Entity
@Table(name = "system_configurations")
public class SystemConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", unique = true, nullable = false)
    public String configKey;

    @Column(name = "config_value")
    public String configValue;

    @Deprecated
    protected SystemConfiguration() {
    }

    public SystemConfiguration(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

}
