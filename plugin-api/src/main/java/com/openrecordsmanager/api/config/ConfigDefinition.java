package com.openrecordsmanager.api.config;

import com.openrecordsmanager.api.Component;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public final class ConfigDefinition<T> implements Component {
    private final String key;
    private final ConfigValueType<T> type;
    private final String name;
    private final String description;
    private final T defaultValue;

    private ConfigDefinition(
            @NonNull String key,
            @NonNull ConfigValueType<T> type,
            @NonNull String name,
            @NonNull String description,
            T defaultValue
    ) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(name, "name cannot be null");

        this.key = key;
        this.name = name;
        this.type = type;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public static <M> Builder<M> builder(String id, ConfigValueType<M> type) {
        return new Builder<>(id, type);
    }

    public String key() {
        return this.key;
    }

    public ConfigValueType<T> type() {
        return this.type;
    }

    public String name() {
        return this.name;
    }

    public String description() {
        return this.description;
    }

    public T defaultValue() {
        return this.defaultValue;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - default: %s", this.key, this.name, this.defaultValue);
    }

    public static class Builder<T> {
        private final String key;
        private final ConfigValueType<T> type;
        private String name;
        private String description = "";
        private T defaultValue = null;

        public Builder(String key, ConfigValueType<T> type) {
            this.key = key;
            this.type = type;
            this.name = key;
        }

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public ConfigDefinition<T> build() {
            return new ConfigDefinition<>(this.key, this.type, this.name, this.description, this.defaultValue);
        }
    }
}
