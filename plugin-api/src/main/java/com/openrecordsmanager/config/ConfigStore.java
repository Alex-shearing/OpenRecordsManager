package com.openrecordsmanager.config;

public interface ConfigStore {
    <T> T getProperty(ConfigProperty<T> key);
}
