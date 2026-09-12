package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.InputValidationException;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.list.ListElementRepository;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Bridges wire/storage {@link JsonNode} and domain {@code T} for object properties.
 */
@Component
public class PropertyValueCodec {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private final ListElementRepository listElementRepository;

    public PropertyValueCodec(ListElementRepository listElementRepository) {
        this.listElementRepository = listElementRepository;
    }

    @PostConstruct
    void install() {
        ObjectPropertyHolder.installCodec(this);
    }

    @PreDestroy
    void uninstall() {
        ObjectPropertyHolder.uninstallCodec(this);
    }

    /**
     * Wire {@link JsonNode} → domain {@code T} (resolves list ids).
     */
    public <T> @Nullable T parse(ObjectProperty<T> property, @Nullable JsonNode wire) {
        if (wire == null || wire.isNull()) {
            return null;
        }

        PropertyType<T> type = property.getType();
        if (!type.allowsList()) {
            return type.parse(wire);
        }

        ListType listType = requireListType(property);
        Object domain;
        if (type == PropertyType.LIST_ITEM) {
            domain = resolveElement(listType, wire);
        } else if (type == PropertyType.LIST_MULTIPLE) {
            if (!wire.isArray()) {
                throw new InputValidationException(Map.of(
                        property.getId().toString(),
                        "list_multiple values must be a JSON array of list element ids"
                ));
            }
            List<ListElement> resolved = new ArrayList<>(wire.size());
            for (JsonNode item : wire) {
                resolved.add(resolveElement(listType, item));
            }
            domain = resolved;
        } else {
            domain = type.parse(wire);
        }

        T parsed = type.parseValue(domain);
        if (parsed == null) {
            throw new InputValidationException(Map.of(
                    property.getId().toString(),
                    "unable to parse value as " + type.getName()
            ));
        }
        return parsed;
    }

    /**
     * Domain → storage {@link JsonNode} (list ids as text / array).
     * List properties require {@link ObjectProperty#getListType()}.
     */
    public JsonNode toStored(ObjectProperty<?> property, @Nullable Object domainValue) {
        if (domainValue == null) {
            return NullNode.getInstance();
        }
        if (property.getType().allowsList()) {
            requireListType(property);
            return toIdNode(domainValue);
        }
        return PropertyType.toTree(domainValue);
    }

    /**
     * Storage {@link JsonNode} → domain (hydrate list ids).
     */
    public @Nullable Object hydrate(ObjectProperty<?> property, @Nullable JsonNode stored) {
        if (stored == null || stored.isNull()) {
            return null;
        }
        if (!property.getType().allowsList()) {
            return property.getType().parse(stored);
        }

        ListType listType = requireListType(property);

        if (property.getType() == PropertyType.LIST_ITEM) {
            return resolveElement(listType, stored);
        }

        if (property.getType() == PropertyType.LIST_MULTIPLE) {
            if (!stored.isArray()) {
                return null;
            }
            if (stored.isEmpty()) {
                return List.of();
            }
            List<ListElement> hydrated = new ArrayList<>(stored.size());
            for (JsonNode item : stored) {
                hydrated.add(resolveElement(listType, item));
            }
            return hydrated;
        }

        return property.getType().parse(stored);
    }

    /**
     * Domain → API wire {@link JsonNode}.
     */
    public JsonNode encode(ObjectProperty<?> property, @Nullable Object domainValue) {
        if (domainValue == null) {
            return NullNode.getInstance();
        }
        if (property.getType().allowsList()) {
            return toIdNode(domainValue);
        }
        return PropertyType.toTree(domainValue);
    }

    private JsonNode toIdNode(Object domainValue) {
        if (domainValue instanceof IListElement element) {
            return NODES.stringNode(element.getId().toString());
        }
        if (domainValue instanceof Collection<?> collection) {
            ArrayNode array = NODES.arrayNode(collection.size());
            for (Object item : collection) {
                if (item instanceof IListElement element) {
                    array.add(element.getId().toString());
                } else if (item != null) {
                    array.add(item.toString());
                }
            }
            return array;
        }
        return NODES.stringNode(domainValue.toString());
    }

    private ListElement resolveElement(ListType listType, JsonNode raw) {
        if (raw.isNull()) {
            throw new InputValidationException(Map.of("value", "list element id is required"));
        }
        if (raw.isObject()) {
            throw new InputValidationException(Map.of(
                    "value",
                    "list element values must be resource id strings, not objects"
            ));
        }
        ResourceIdentifier elementId = ResourceIdentifier.valueOf(raw.asString().trim());
        return this.listElementRepository.getElement(listType.getId(), elementId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST_ELEMENT, elementId));
    }

    private static ListType requireListType(ObjectProperty<?> property) {
        ListType listType = property.getListType();
        if (listType == null) {
            throw new InputValidationException(Map.of(
                    property.getId().toString(),
                    "list property is missing listType"
            ));
        }
        return listType;
    }

}
