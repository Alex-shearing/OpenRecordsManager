package com.openrecordsmanager.api.config;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.template.property.PropertyType;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public record ConfigType<T>(
        String key,
        PropertyType<T> type,
        @Nullable T defaultValue
) implements Component {

    public ConfigType {
        Objects.requireNonNull(key, "Property 'key' must not be null");
        Objects.requireNonNull(type, "Property 'type' must not be null");
        if (!type.supportsConfig()) {
            throw new IllegalArgumentException("PropertyType '" + type.getName() + "' does not support configuration");
        }
    }

    public static <M> Builder<M> builder(String id, PropertyType<M> type) {
        return new Builder<>(id, type);
    }

    @Override
    public String toString() {
        return String.format("%s - default: %s", this.key, this.defaultValue);
    }

    public static class Builder<T> {
        private final String key;
        private final PropertyType<T> type;
        @Nullable
        private T defaultValue = null;

        public Builder(String key, PropertyType<T> type) {
            this.key = key;
            this.type = type;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public ConfigType<T> build() {
            return new ConfigType<>(this.key, this.type, this.defaultValue);
        }
    }
}
