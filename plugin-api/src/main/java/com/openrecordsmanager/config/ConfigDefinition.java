package com.openrecordsmanager.config;

import com.openrecordsmanager.RegisterableComponent;

import java.util.Locale;
import java.util.Objects;

public final class ConfigDefinition<T> implements RegisterableComponent {
    private final String id;
    private final ConfigValueType<T> type;
    private final String name;
    private final String description;
    private final T defaultValue;
    private final String alias;

    private ConfigDefinition(String id, ConfigValueType<T> type, String name, String description, T defaultValue, String alias) {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(name, "name cannot be null");

        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.defaultValue = defaultValue;
        this.alias = alias;
    }

    public static <M> Builder<M> builder(String id, ConfigValueType<M> type) {
        return new Builder<>(id, type);
    }

    public String id() {
        return id;
    }

    public String getEnvName() {
        return this.id.replace('.', '_').toUpperCase(Locale.ROOT);
    }

    public ConfigValueType<T> type() {
        return type;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String alias() {
        return alias;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public boolean isKey(String id) {
        return this.id.equals(id) || (this.alias != null && this.alias.equals(id));
    }

    @Override
    public String toString() {
        return "ConfigProperty{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", defaultValue=" + defaultValue +
                '}';
    }

    public static class Builder<T> {
        private final String id;
        private ConfigValueType<T> type;
        private String name;
        private String description;
        private T defaultValue;
        private String alias;

        public Builder(String id, ConfigValueType<T> type) {
            this.id = id;
            this.type = type;
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

        public Builder<T> alias(String alias) {
            this.alias = alias;
            return this;
        }

        public ConfigDefinition<T> build() {
            return new ConfigDefinition<>(this.id, this.type, this.name, this.description, this.defaultValue, this.alias);
        }
    }
}
