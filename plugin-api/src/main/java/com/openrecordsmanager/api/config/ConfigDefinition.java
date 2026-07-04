package com.openrecordsmanager.api.config;

import com.openrecordsmanager.api.Component;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public record ConfigDefinition<T>(
        String key,
        ConfigValueType<T> type,
        String name,
        String description,
        @Nullable T defaultValue
) implements Component {

    public ConfigDefinition {
        Objects.requireNonNull(key, "Property 'key' must not be null");
        Objects.requireNonNull(type, "Property 'type' must not be null");
        Objects.requireNonNull(name, "Property 'name' must not be null");
    }

    public static <M> Builder<M> builder(String id, ConfigValueType<M> type) {
        return new Builder<>(id, type);
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
        @Nullable
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
            T typedDefault = this.defaultValue;
            return new ConfigDefinition<>(this.key, this.type, this.name, this.description, typedDefault);
        }
    }
}
