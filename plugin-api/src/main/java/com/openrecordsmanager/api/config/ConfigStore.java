package com.openrecordsmanager.api.config;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

public interface ConfigStore {
    <T> @Nullable T getValue(ConfigType<T> key);

    default <T> Optional<T> getOptional(ConfigType<T> key) {
        return Optional.ofNullable(this.getValue(key));
    }

    default <T> T getOrDefault(ConfigType<T> key, T defaultValue) {
        T value = this.getValue(key);
        return value != null ? value : defaultValue;
    }

    default <T> T getOrThrow(ConfigType<T> key) {
        T value = this.getValue(key);
        if (value == null) {
            throw new IllegalStateException(String.format("No value found for key '%s'", key.name()));
        }
        return value;
    }
}
