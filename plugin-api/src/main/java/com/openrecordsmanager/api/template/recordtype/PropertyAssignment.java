package com.openrecordsmanager.api.template.recordtype;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record PropertyAssignment<T>(
        ComponentReference<ObjectPropertyTemplate<T>> property,
        @Nullable T defaultValue
) {
    public static <K> PropertyAssignment<K> of(ComponentReference<ObjectPropertyTemplate<K>> property) {
        return new PropertyAssignment<>(property, null);
    }

    public static <K> PropertyAssignment<K> of(ComponentReference<ObjectPropertyTemplate<K>> property, @Nullable K defaultValue) {
        return new PropertyAssignment<>(property, defaultValue);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static PropertyAssignment<?> ofUnknown(ComponentReference<? extends ObjectPropertyTemplate<?>> property) {
        return new PropertyAssignment(property, null);
    }

    public static class ListDeserializer extends StdDeserializer<List<PropertyAssignment<?>>> {
        public ListDeserializer() {
            super(List.class);
        }

        @Override
        public List<PropertyAssignment<?>> deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            JsonNode node = p.readValueAsTree();
            if (node == null || node.isNull()) {
                return List.of();
            }
            if (node.isArray()) {
                List<PropertyAssignment<?>> assignments = new ArrayList<>();
                for (JsonNode item : node) {
                    assignments.add(ctxt.readTreeAsValue(item, PropertyAssignment.class));
                }
                return assignments;
            }
            if (node.isObject()) {
                List<PropertyAssignment<?>> assignments = new ArrayList<>();
                for (Map.Entry<String, JsonNode> entry : node.properties()) {
                    assignments.add(fromMapEntry(entry.getKey(), entry.getValue(), ctxt));
                }
                return assignments;
            }
            return ctxt.reportInputMismatch(this, "Expected object or array for properties");
        }

        @SuppressWarnings("unchecked")
        private static <T> PropertyAssignment<T> fromMapEntry(String key, JsonNode value, DeserializationContext ctxt) {
            ComponentReference<ObjectPropertyTemplate<T>> property =
                    (ComponentReference<ObjectPropertyTemplate<T>>) ComponentReference.valueOf(key);
            T defaultValue = value.isNull() ? null : (T) ctxt.readTreeAsValue(value, Object.class);
            return new PropertyAssignment<>(property, defaultValue);
        }
    }
}
