package com.openrecordsmanager.property;

import com.openrecordsmanager.list.IListElement;

import java.util.*;

public abstract class PropertyType<T> {
    public static Map<String, PropertyType<?>> TYPES = new HashMap<>(7);

    public static final PropertyType<String> CALCULATED = new PropertyType<>("calculated") {
        @Override
        public String validate(PropertyDefinition<String> definition, Object value) {
            return "tba";
        }
    };

    public static final PropertyType<String> STRING = new PropertyType<>("string") {
        @Override
        public String validate(PropertyDefinition<String> definition, Object value) {
            return value instanceof String v ? v : null;
        }
    };

    public static final PropertyType<Integer> INTEGER = new PropertyType<>("integer") {
        @Override
        public Integer validate(PropertyDefinition<Integer> definition, Object value) {
            return value instanceof Integer v ? v : null;
        }
    };

    public static final PropertyType<Double> DOUBLE = new PropertyType<>("double") {
        @Override
        public Double validate(PropertyDefinition<Double> definition, Object value) {
            return value instanceof Double v ? v : null;
        }
    };

    public static final PropertyType<Boolean> BOOLEAN = new PropertyType<>("boolean") {
        @Override
        public Boolean validate(PropertyDefinition<Boolean> definition, Object value) {
            return value instanceof Boolean v ? v : null;
        }
    };

    public static final PropertyType<IListElement> LIST_ITEM = new PropertyType<>("list_item") {
        @Override
        public IListElement validate(PropertyDefinition<IListElement> definition, Object value) {
            return value instanceof IListElement v ? v : null;
        }
    };

    public static final PropertyType<List<IListElement>> LIST_MULTIPLE = new PropertyType<>("list_multiple") {
        @SuppressWarnings("unchecked")
        @Override
        public List<IListElement> validate(PropertyDefinition<List<IListElement>> definition, Object value) {
            if (value instanceof Set<?> v) {
                if (v.isEmpty()) {
                    return (List<IListElement>) v;
                }
                for (Object o : v) {
                    if (!(o instanceof IListElement)) {
                        return null;
                    }
                }
                return (List<IListElement>) v;
            }
            return null;
        }
    };

    public static final PropertyType<Date> DATE = new PropertyType<>("date") {
        @Override
        public Date validate(PropertyDefinition<Date> definition, Object value) {
            return value instanceof Date v ? v : null;
        }
    };

    public final String name;

    private PropertyType(String name) {
        this.name = name;
        TYPES.put(name, this);
    }

    public abstract T validate(PropertyDefinition<T> definition, Object value);
}