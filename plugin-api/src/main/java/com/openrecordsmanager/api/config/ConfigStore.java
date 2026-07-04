package com.openrecordsmanager.api.config;

import org.jspecify.annotations.Nullable;

public interface ConfigStore {
    <T> @Nullable T getProperty(ConfigDefinition<T> key);

    default <T> T getProperty(ConfigDefinition<T> key, T defaultValue) {
        T value = this.getProperty(key);
        return value != null ? value : defaultValue;
    }

    default <T> T getPropertyOrThrow(ConfigDefinition<T> key) {
        T value = this.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(String.format("No value found for key '%s'", key.name()));
        }
        return value;
    }
}
