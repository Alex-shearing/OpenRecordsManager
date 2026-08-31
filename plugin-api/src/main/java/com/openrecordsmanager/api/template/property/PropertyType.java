package com.openrecordsmanager.api.template.property;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.template.list.IListElement;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.*;

@SuppressWarnings("unused")
public abstract class PropertyType<T> {
    public static final Map<String, PropertyType<?>> TYPES = new HashMap<>(16);
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    public static final PropertyType<String> CALCULATED = new PropertyType<>("calculated", String.class, false) {
        @Override
        protected String coerce(@Nullable Object value) {
            return "tba";
        }
    };

    public static final PropertyType<String> STRING = new PropertyType<>("string", String.class, true) {
        @Override
        protected @Nullable String coerce(@Nullable Object value) {
            return value != null ? value.toString() : null;
        }
    };

    public static final PropertyType<Long> NUMBER = new PropertyType<>("number", Long.class, true) {
        @Override
        protected @Nullable Long coerce(@Nullable Object value) {
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String s) {
                try {
                    return Long.valueOf(s.trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
    };

    public static final PropertyType<Double> DECIMAL = new PropertyType<>("decimal", Double.class, true) {
        @Override
        protected @Nullable Double coerce(@Nullable Object value) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String s) {
                try {
                    return Double.valueOf(s.trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
    };

    public static final PropertyType<Boolean> BOOLEAN = new PropertyType<>("boolean", Boolean.class, true) {
        @Override
        protected @Nullable Boolean coerce(@Nullable Object value) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof String s) {
                return Boolean.valueOf(s.trim());
            }
            return null;
        }
    };

    public static final PropertyType<UUID> UUID = new PropertyType<>("uuid", UUID.class, true) {
        @Override
        protected @Nullable UUID coerce(@Nullable Object value) {
            if (value instanceof UUID uuid) {
                return uuid;
            }
            if (value instanceof String s) {
                try {
                    return java.util.UUID.fromString(s.trim());
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
            return null;
        }
    };

    public static final PropertyType<Object> OBJECT = new PropertyType<>("object", Object.class, true) {
        @Override
        protected @Nullable Object coerce(@Nullable Object value) {
            throw new UnsupportedOperationException("object config values are not supported");
        }
    };

    public static final PropertyType<IListElement> LIST_ITEM = new PropertyType<>("list_item", IListElement.class, false) {
        @Override
        protected @Nullable IListElement coerce(@Nullable Object value) {
            return value instanceof IListElement v ? v : null;
        }
    };

    public static final PropertyType<Collection<IListElement>> LIST_MULTIPLE = new PropertyType<>("list_multiple", (Class<Collection<IListElement>>) (Class<?>) Collection.class, false) {
        @SuppressWarnings("unchecked")
        @Override
        protected @Nullable Collection<IListElement> coerce(@Nullable Object value) {
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

    public static final PropertyType<Instant> DATE = new PropertyType<>("date", Instant.class, false) {
        @Override
        protected @Nullable Instant coerce(@Nullable Object value) {
            if (value instanceof Instant instant) {
                return instant;
            }
            if (value instanceof Date date) {
                return date.toInstant();
            }
            return null;
        }
    };

    public static final PropertyType<List<String>> STRING_LIST = new PropertyType<>("string_list", (Class<List<String>>) (Class<?>) List.class, true) {
        @Override
        protected @Nullable List<String> coerce(@Nullable Object value) {
            return switch (value) {
                case null -> null;
                case Collection<?> collection -> collection.stream().map(Object::toString).toList();
                case String[] array -> Arrays.stream(array).map(Object::toString).toList();
                case String s -> {
                    if (s.isBlank()) {
                        yield List.of();
                    }
                    yield Arrays.stream(s.split("[;,]"))
                            .map(String::trim)
                            .filter(part -> !part.isEmpty())
                            .toList();
                }
                default -> List.of(value.toString());
            };
        }
    };

    public static final PropertyType<List<Integer>> INT_LIST = new PropertyType<>("int_list", (Class<List<Integer>>) (Class<?>) List.class, true) {
        @Override
        protected @Nullable List<Integer> coerce(@Nullable Object value) {
            return switch (value) {
                case Collection<?> collection -> collection.stream()
                        .map(item -> item instanceof Number n ? n.intValue() : Integer.parseInt(item.toString()))
                        .toList();
                case Integer[] array -> Arrays.stream(array).toList();
                case String s -> {
                    if (s.isBlank()) {
                        yield List.of();
                    }
                    yield Arrays.stream(s.split("[;,]"))
                            .map(String::trim)
                            .filter(part -> !part.isEmpty())
                            .map(Integer::valueOf)
                            .toList();
                }
                case null, default -> null;
            };
        }
    };

    private final String name;
    public final Class<T> valueClass;
    private final boolean configSupported;

    protected PropertyType(String name, Class<T> valueClass, boolean configSupported) {
        this.name = name;
        this.valueClass = valueClass;
        this.configSupported = configSupported;
        TYPES.put(name, this);
    }

    protected abstract @Nullable T coerce(@Nullable Object value);

    @SuppressWarnings("unchecked")
    public @Nullable T parseValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            try {
                return JSON.readValue(s, JSON.constructType(this.valueClass));
            } catch (Exception ignored) {
                // fall through to plain string handling/coercion
            }
            return this.coerce(value);
        }
        if (this.valueClass.isInstance(value)) {
            return (T) value;
        }
        try {
            return JSON.convertValue(value, this.valueClass);
        } catch (Exception ignored) {
            // fall through to type-specific coercion
        }
        return this.coerce(value);
    }

    public boolean supportsConfig() {
        return this.configSupported;
    }

    public boolean allowsList() {
        return this == LIST_ITEM || this == LIST_MULTIPLE;
    }

    public String getName() {
        return this.name;
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

    @Override
    public String toString() {
        return this.name;
    }
}
