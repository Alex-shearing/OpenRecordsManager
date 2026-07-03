package com.openrecordsmanager.api.property;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.list.IListElement;

import java.util.*;

@SuppressWarnings("unused")
public abstract class PropertyType<T> {
    public static final Map<String, PropertyType<?>> TYPES = new HashMap<>(7);

    public static final PropertyType<String> CALCULATED = new PropertyType<>("calculated") {
        @Override
        public String cast(Object value) {
            return "tba";
        }
    };

    public static final PropertyType<String> STRING = new PropertyType<>("string") {
        @Override
        public String cast(Object value) {
            return value != null ? value.toString() : "";
        }
    };

    public static final PropertyType<Long> NUMBER = new PropertyType<>("number") {
        @Override
        public Long cast(Object value) {
            return value instanceof Number v ? v.longValue() : null;
        }
    };

    public static final PropertyType<Double> DECIMAL = new PropertyType<>("decimal") {
        @Override
        public Double cast(Object value) {
            return value instanceof Number v ? v.doubleValue() : null;
        }
    };

    public static final PropertyType<Boolean> BOOLEAN = new PropertyType<>("boolean") {
        @Override
        public Boolean cast(Object value) {
            return value instanceof Boolean v ? v : null;
        }
    };

    public static final PropertyType<IListElement> LIST_ITEM = new PropertyType<>("list_item") {
        @Override
        public IListElement cast(Object value) {
            return value instanceof IListElement v ? v : null;
        }
    };

    public static final PropertyType<Collection<IListElement>> LIST_MULTIPLE = new PropertyType<>("list_multiple") {
        @SuppressWarnings("unchecked")
        @Override
        public Collection<IListElement> cast(Object value) {
            if (value instanceof Collection<?> v) {
                if (v.isEmpty()) {
                    return (Collection<IListElement>) v;
                }
                for (Object o : v) {
                    if (!(o instanceof IListElement)) {
                        return null;
                    }
                }
                return (Collection<IListElement>) v;
            }
            return null;
        }
    };

    public static final PropertyType<Date> DATE = new PropertyType<>("date") {
        @Override
        public Date cast(Object value) {
            return value instanceof Date v ? v : null;
        }
    };

    public final String name;

    private PropertyType(String name) {
        this.name = name;
        TYPES.put(name, this);
    }

    public abstract T cast(Object value);

    public boolean allowsList() {
        return this == LIST_ITEM || this == LIST_MULTIPLE;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertyType<?> that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @JsonCreator
    private static PropertyType<?> fromString(String key) {
        PropertyType<?> type = TYPES.get(key);
        if (type == null) {
            throw new IllegalArgumentException("Unknown PropertyType key: " + key);
        }
        return type;
    }
}