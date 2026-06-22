package com.openrecordsmanager.api.config;

public interface ConfigStore {
    <T> T getProperty(ConfigDefinition<T> key);
}
