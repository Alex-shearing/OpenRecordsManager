package com.openrecordsmanager.api.template.property;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.CatalogAccess;
import com.openrecordsmanager.api.template.list.IListElement;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.NullNode;

import java.time.Instant;
import java.util.*;

/**
 * Wire/storage value parsing for object properties and config keys.
 * All inputs go through {@link #parse(JsonNode)}; list id resolution uses a
 * list-scoped {@link CatalogAccess} when provided.
 */
@SuppressWarnings("unused")
public abstract class PropertyType<T> {
    public static final Map<String, PropertyType<?>> TYPES = new HashMap<>(16);
    
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    public static final PropertyType<String> STRING = new PropertyType<>("string", String.class, true) {
        @Override
        protected @Nullable String coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            return node.isString() ? node.stringValue() : null;
        }
    };

    public static final PropertyType<Long> NUMBER = new PropertyType<>("number", Long.class, true) {
        @Override
        protected @Nullable Long coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            if (node.isIntegralNumber()) {
                return node.longValue();
            }
            if (node.isString()) {
                try {
                    return Long.valueOf(node.stringValue().trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
    };

    public static final PropertyType<Double> DECIMAL = new PropertyType<>("decimal", Double.class, true) {
        @Override
        protected @Nullable Double coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            if (node.isNumber()) {
                return node.doubleValue();
            }
            if (node.isString()) {
                try {
                    return Double.valueOf(node.stringValue().trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
    };

    public static final PropertyType<Boolean> BOOLEAN = new PropertyType<>("boolean", Boolean.class, true) {
        @Override
        protected @Nullable Boolean coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            if (node.isBoolean()) {
                return node.booleanValue();
            }
            if (node.isString()) {
                String trimmed = node.stringValue().trim();
                if (trimmed.equalsIgnoreCase("true")) {
                    return true;
                }
                if (trimmed.equalsIgnoreCase("false")) {
                    return false;
                }
            }
            return null;
        }
    };

    public static final PropertyType<UUID> UUID = new PropertyType<>("uuid", UUID.class, true) {
        @Override
        protected @Nullable UUID coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            if (!node.isString()) {
                return null;
            }
            try {
                return java.util.UUID.fromString(node.stringValue().trim());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    };

    /**
     * Structured config blobs (e.g. database settings) as JSON trees.
     */
    public static final PropertyType<JsonNode> OBJECT = new PropertyType<>("object", JsonNode.class, true) {
        @Override
        protected @Nullable JsonNode coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            return node.isNull() ? null : node;
        }
    };

    public static final PropertyType<IListElement> LIST_ITEM = new PropertyType<>("list_item", IListElement.class, false) {
        @Override
        protected @Nullable IListElement coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            if (catalog == null || node.isObject() || !node.isString()) {
                return null;
            }
            return catalog.resolveElement(ResourceIdentifier.valueOf(node.stringValue().trim()));
        }
    };

    public static final PropertyType<Instant> DATE = new PropertyType<>("date", Instant.class, false) {
        @Override
        protected @Nullable Instant coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            if (!node.isString()) {
                return null;
            }
            try {
                return Instant.parse(node.stringValue().trim());
            } catch (Exception ignored) {
                return null;
            }
        }
    };

    @SuppressWarnings("unchecked")
    public static final PropertyType<Collection<IListElement>> LIST_MULTIPLE = new PropertyType<>(
            "list_multiple",
            (Class<Collection<IListElement>>) (Class<?>) Collection.class,
            false
    ) {
        @Override
        protected @Nullable Collection<IListElement> coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            return coerceListNode(node, LIST_ITEM, catalog);
        }
    };

    @SuppressWarnings("unchecked")
    public static final PropertyType<List<String>> STRING_LIST = new PropertyType<>(
            "string_list",
            (Class<List<String>>) (Class<?>) List.class,
            true
    ) {
        @Override
        protected @Nullable List<String> coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            return coerceListNode(node, STRING, catalog);
        }
    };

    @SuppressWarnings("unchecked")
    public static final PropertyType<List<Long>> NUMBER_LIST = new PropertyType<>(
            "number_list",
            (Class<List<Long>>) (Class<?>) List.class,
            true
    ) {
        @Override
        protected @Nullable List<Long> coerceNode(JsonNode node, @Nullable CatalogAccess catalog) {
            return coerceListNode(node, NUMBER, catalog);
        }
    };

    /**
     * Array or {@code ;}/{@code ,}-delimited string → list of elements via {@code elementType.parse}.
     */
    private static <E> @Nullable List<E> coerceListNode(
            JsonNode node,
            PropertyType<E> elementType,
            @Nullable CatalogAccess catalog
    ) {
        if (node.isArray()) {
            List<E> out = new ArrayList<>(node.size());
            for (JsonNode item : node) {
                E parsed = elementType.parse(item, catalog);
                if (parsed == null) {
                    return null;
                }
                out.add(parsed);
            }
            return out;
        }
        if (node.isString()) {
            String s = node.stringValue();
            if (s.isBlank()) {
                return List.of();
            }
            List<E> out = new ArrayList<>();
            for (String part : s.split("[;,]")) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                E parsed = elementType.parse(JSON.getNodeFactory().stringNode(trimmed), catalog);
                if (parsed == null) {
                    return null;
                }
                out.add(parsed);
            }
            return out;
        }
        return null;
    }

    private final String name;
    public final Class<T> valueClass;
    private final boolean configSupported;

    protected PropertyType(String name, Class<T> valueClass, boolean configSupported) {
        this.name = name;
        this.valueClass = valueClass;
        this.configSupported = configSupported;
        TYPES.put(name, this);
    }

    /**
     * Wire/storage {@link JsonNode} → domain {@code T}. Fail closed on shape mismatch.
     * List types require a non-null {@code catalog} to resolve element ids.
     */
    protected abstract @Nullable T coerceNode(JsonNode node, @Nullable CatalogAccess catalog);

    /**
     * Primary parse entry: wire/storage {@link JsonNode} → domain {@code T}.
     */
    public @Nullable T parse(@Nullable JsonNode node) {
        return this.parse(node, null);
    }

    /**
     * Wire/storage {@link JsonNode} → domain {@code T}, resolving list ids via {@code catalog}.
     */
    public @Nullable T parse(@Nullable JsonNode node, @Nullable CatalogAccess catalog) {
        if (node == null || node instanceof NullNode || node.isNull()) {
            return null;
        }
        return this.coerceNode(node, catalog);
    }

    /**
     * Convert an env/file raw string into a {@link JsonNode} for {@link #parse(JsonNode)}.
     * JSON documents/arrays/quoted strings are parsed as JSON; everything else becomes a string node
     * (so {@code true}, {@code 42}, and delimited lists still work via {@code coerceNode}).
     */
    public static JsonNode nodeFromRawString(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.isEmpty()) {
            char first = trimmed.charAt(0);
            if (first == '"' || first == '{' || first == '[') {
                try {
                    return JSON.readTree(trimmed);
                } catch (Exception ignored) {
                    // fall through to string node
                }
            }
        }
        return JSON.getNodeFactory().stringNode(raw);
    }

    public static JsonNode toTree(@Nullable Object domain) {
        if (domain == null) {
            return NullNode.getInstance();
        }
        if (domain instanceof JsonNode node) {
            return node;
        }
        return JSON.valueToTree(domain);
    }

    public boolean supportsConfig() {
        return this.configSupported;
    }

    public boolean allowsList() {
        return this == LIST_ITEM || this == LIST_MULTIPLE;
    }

    @JsonValue
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
