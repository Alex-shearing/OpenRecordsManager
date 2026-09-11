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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Resolves list property inputs (resource id strings) to {@link ListElement} entities,
 * hydrates stored id strings after JSON persistence, and converts domain values to API shapes.
 */
@Component
public class ListElementPropertyResolver {
    private static volatile @Nullable ListElementPropertyResolver INSTANCE;

    private final ListElementRepository listElementRepository;

    public ListElementPropertyResolver(ListElementRepository listElementRepository) {
        this.listElementRepository = listElementRepository;
    }

    @PostConstruct
    void register() {
        INSTANCE = this;
    }

    @PreDestroy
    void unregister() {
        if (INSTANCE == this) {
            INSTANCE = null;
        }
    }

    public static @Nullable ListElementPropertyResolver getInstance() {
        return INSTANCE;
    }

    /**
     * Resolve raw API / untyped input into domain values suitable for {@link PropertyType#parseValue}.
     * Non-list properties are returned unchanged.
     */
    public @Nullable Object resolveInput(ObjectProperty<?> property, @Nullable Object raw) {
        PropertyType<?> type = property.getType();
        if (!type.allowsList()) {
            return raw;
        }
        if (raw == null) {
            return null;
        }

        ListType listType = requireListType(property);

        if (type == PropertyType.LIST_ITEM) {
            return resolveElement(listType, raw);
        }

        if (type == PropertyType.LIST_MULTIPLE) {
            if (!(raw instanceof Collection<?> collection)) {
                throw new InputValidationException(Map.of(
                        property.getId().toString(),
                        "list_multiple values must be a collection of list element ids"
                ));
            }
            List<ListElement> resolved = new ArrayList<>(collection.size());
            for (Object item : collection) {
                resolved.add(resolveElement(listType, item));
            }
            return resolved;
        }

        return raw;
    }

    /**
     * Convert a domain list value to a persistence-friendly form (resource id string / list of strings).
     * Non-list values are returned unchanged.
     */
    public @Nullable Object toStoredValue(ObjectProperty<?> property, @Nullable Object domainValue) {
        if (!property.getType().allowsList() || domainValue == null) {
            return domainValue;
        }

        if (domainValue instanceof IListElement element) {
            return elementId(element);
        }

        if (domainValue instanceof Collection<?> collection) {
            List<String> ids = new ArrayList<>(collection.size());
            for (Object item : collection) {
                if (item instanceof IListElement element) {
                    ids.add(elementId(element));
                } else if (item != null) {
                    ids.add(item.toString());
                }
            }
            return ids;
        }

        return domainValue.toString();
    }

    /**
     * Hydrate a value loaded from JSON (id string / list of strings) back to {@link ListElement}s.
     * Already-hydrated {@link IListElement} values pass through.
     */
    public @Nullable Object hydrateStored(ObjectProperty<?> property, @Nullable Object stored) {
        if (!property.getType().allowsList() || stored == null) {
            return stored;
        }

        if (stored instanceof IListElement) {
            return stored;
        }

        ListType listType = property.getListType();
        if (listType == null) {
            return stored;
        }

        if (property.getType() == PropertyType.LIST_ITEM) {
            if (stored instanceof String || stored instanceof ResourceIdentifier) {
                return resolveElement(listType, stored);
            }
            return stored;
        }

        if (property.getType() == PropertyType.LIST_MULTIPLE && stored instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return List.of();
            }
            if (collection.iterator().next() instanceof IListElement) {
                return stored;
            }
            List<ListElement> hydrated = new ArrayList<>(collection.size());
            for (Object item : collection) {
                hydrated.add(resolveElement(listType, item));
            }
            return hydrated;
        }

        return stored;
    }

    /**
     * Convert domain list values to API-friendly resource id string(s).
     */
    public static @Nullable Object toApiValue(ObjectProperty<?> property, @Nullable Object domainValue) {
        if (domainValue == null || !property.getType().allowsList()) {
            return domainValue;
        }

        ListElementPropertyResolver resolver = INSTANCE;
        if (resolver != null) {
            return resolver.toStoredValue(property, domainValue);
        }

        if (domainValue instanceof IListElement element) {
            return elementId(element);
        }
        if (domainValue instanceof Collection<?> collection) {
            return collection.stream()
                    .map(item -> item instanceof IListElement el ? elementId(el) : String.valueOf(item))
                    .toList();
        }
        return domainValue;
    }

    public static @Nullable Object resolveInputStatic(ObjectProperty<?> property, @Nullable Object raw) {
        if (!property.getType().allowsList()) {
            return raw;
        }
        ListElementPropertyResolver resolver = INSTANCE;
        if (resolver == null) {
            // Unit tests may set live IListElement instances without Spring.
            return raw;
        }
        return resolver.resolveInput(property, raw);
    }

    public static @Nullable Object hydrateStoredStatic(ObjectProperty<?> property, @Nullable Object stored) {
        if (!property.getType().allowsList() || stored == null) {
            return stored;
        }
        if (stored instanceof IListElement) {
            return stored;
        }
        if (stored instanceof Collection<?> collection
                && !collection.isEmpty()
                && collection.iterator().next() instanceof IListElement) {
            return stored;
        }
        ListElementPropertyResolver resolver = INSTANCE;
        if (resolver == null) {
            return stored;
        }
        return resolver.hydrateStored(property, stored);
    }

    private ListElement resolveElement(ListType listType, @Nullable Object raw) {
        if (raw == null) {
            throw new InputValidationException(Map.of("value", "list element id is required"));
        }
        if (raw instanceof ListElement element) {
            validateMembership(listType, element);
            return element;
        }
        if (raw instanceof IListElement) {
            throw new InputValidationException(Map.of(
                    "value",
                    "unsupported list element implementation: " + raw.getClass().getName()
            ));
        }

        ResourceIdentifier elementId = toResourceIdentifier(raw);
        return this.listElementRepository.getElement(listType.getId(), elementId)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.LIST_ELEMENT, elementId));
    }

    private static void validateMembership(ListType listType, ListElement element) {
        if (!listType.getId().equals(element.getParent().getId())) {
            throw new InputValidationException(Map.of(
                    "value",
                    "list element " + element.getId() + " does not belong to list " + listType.getId()
            ));
        }
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

    private static ResourceIdentifier toResourceIdentifier(Object raw) {
        if (raw instanceof ResourceIdentifier id) {
            return id;
        }
        try {
            return ResourceIdentifier.valueOf(raw.toString().trim());
        } catch (IllegalArgumentException ex) {
            throw new InputValidationException(Map.of(
                    "value",
                    "invalid list element id: " + raw
            ));
        }
    }

    private static String elementId(IListElement element) {
        if (element instanceof ListElement listElement) {
            return listElement.getId().toString();
        }
        throw new IllegalArgumentException("unsupported list element implementation: " + element.getClass().getName());
    }
}
