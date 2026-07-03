package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.api.list.ListElementDefinition;
import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.RecordType;

import java.util.Map;

public class ComponentBinderRegistry {
    public static ComponentBinding<ListDefinition, ListType> LIST = new ListComponentType();
    public static ComponentBinding<ListElementDefinition, ListElement> LIST_ELEMENT = new ListElementComponentType();
    public static ComponentBinding<PropertyDefinition<?>, ObjectProperty<?>> PROPERTY = new ObjectPropertyComponentType();
    public static ComponentBinding<RecordTypeDefinition, RecordType> RECORD_TYPE = new RecordTypeComponentType();

    private static final Map<ComponentType<?>, ComponentBinding<?, ?>> ENTRIES = Map.ofEntries(
            binding(ComponentTypes.LIST, LIST),
            binding(ComponentTypes.LIST_ELEMENT, LIST_ELEMENT),
            binding(ComponentTypes.PROPERTY, PROPERTY),
            binding(ComponentTypes.RECORD_TYPE, RECORD_TYPE)
    );

    @SuppressWarnings("unchecked")
    public static <T extends Component> ComponentBinding<T, ?> get(ComponentType<T> componentType) {
        return (ComponentBinding<T, ?>) ENTRIES.get(componentType);
    }

    private static <T extends Component> Map.Entry<ComponentType<T>, ComponentBinding<T, ?>> binding(
            ComponentType<T> componentType,
            ComponentBinding<T, ?> binding
    ) {
        return Map.entry(componentType, binding);
    }
}
