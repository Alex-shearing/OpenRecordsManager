package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.template.property.PropertyType;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

public record AuditPropertyChange(
        String property,
        @Nullable JsonNode oldValue,
        @Nullable JsonNode newValue
) {
    public static AuditPropertyChange newProperty(String property, @Nullable Object newValue) {
        return new AuditPropertyChange(property, null, tree(newValue));
    }

    public static AuditPropertyChange of(String property, @Nullable Object oldValue, @Nullable Object newValue) {
        return new AuditPropertyChange(property, tree(oldValue), tree(newValue));
    }

    /** Convert an arbitrary audit field value to wire JSON. */
    public static JsonNode tree(@Nullable Object value) {
        if (value == null) {
            return NullNode.getInstance();
        }
        if (value instanceof JsonNode node) {
            return node;
        }
        return PropertyType.toTree(value);
    }
}
