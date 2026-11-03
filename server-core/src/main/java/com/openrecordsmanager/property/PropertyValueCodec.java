package com.openrecordsmanager.property;

import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.template.list.CatalogAccess;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.list.ListElementRepository;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;

import java.util.Collection;

/**
 * Bridges wire/storage {@link JsonNode} and domain {@code T} for object properties.
 * Installed as an ambient service so {@link ManagedObject} can resolve values
 * without constructor injection.
 */
@Component
public class PropertyValueCodec {
    private static final String VALUE_INVALID = "property_value_invalid";

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private static volatile @Nullable PropertyValueCodec installed;

    private final ListElementRepository listElementRepository;

    public PropertyValueCodec(ListElementRepository listElementRepository) {
        this.listElementRepository = listElementRepository;
    }

    @PostConstruct
    void install() {
        PropertyValueCodec.installed = this;
    }

    @PreDestroy
    void uninstall() {
        if (PropertyValueCodec.installed == this) {
            PropertyValueCodec.installed = null;
        }
    }

    public static PropertyValueCodec requireInstalled() {
        PropertyValueCodec codec = PropertyValueCodec.installed;
        if (codec == null) {
            throw new IllegalStateException("PropertyValueCodec is not installed");
        }
        return codec;
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

        CatalogAccess catalog = this.catalogFor(requireListType(property));
        T parsed = type.parse(wire, catalog);
        if (parsed == null) {
            throw ApiException.validationFailed(
                    property.getId().toString(),
                    VALUE_INVALID,
                    type.getName()
            );
        }
        return parsed;
    }

    /**
     * Domain → storage {@link JsonNode} (list ids as text / array).
     * List properties require {@link ObjectProperty#getListType()}.
     */
    public JsonNode toStored(ObjectProperty<?> property, @Nullable Object domainValue) {
        if (property.getType().allowsList() && domainValue != null) {
            requireListType(property);
            return this.toIdNode(domainValue);
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

        CatalogAccess catalog = this.catalogFor(requireListType(property));
        return property.getType().parse(stored, catalog);
    }

    /**
     * Domain → API wire {@link JsonNode}.
     */
    public JsonNode encode(ObjectProperty<?> property, @Nullable Object domainValue) {
        if (domainValue == null) {
            return NullNode.getInstance();
        }
        if (property.getType().allowsList()) {
            return this.toIdNode(domainValue);
        }
        return PropertyType.toTree(domainValue);
    }

    private CatalogAccess catalogFor(ListType listType) {
        return elementId -> this.listElementRepository.getElement(listType.getId(), elementId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST_ELEMENT, elementId));
    }

    private JsonNode toIdNode(Object domainValue) {
        if (domainValue instanceof IListElement element) {
            return NODES.stringNode(element.getId().toString());
        }
        if (domainValue instanceof Collection<?> collection) {
            ArrayNode array = NODES.arrayNode(collection.size());
            for (Object item : collection) {
                if (!(item instanceof IListElement element)) {
                    throw ApiException.validationFailed("value", VALUE_INVALID, "list_multiple");
                }
                array.add(element.getId().toString());
            }
            return array;
        }
        throw ApiException.validationFailed("value", VALUE_INVALID, "list_item");
    }

    private static ListType requireListType(ObjectProperty<?> property) {
        ListType listType = property.getListType();
        if (listType == null) {
            throw ApiException.validationFailed(
                    property.getId().toString(),
                    VALUE_INVALID,
                    property.getType().getName()
            );
        }
        return listType;
    }

}
