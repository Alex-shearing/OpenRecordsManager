package com.openrecordsmanager.config;

public interface ConfigStore {
    <T> T getProperty(ConfigDefinition<T> key);
}
