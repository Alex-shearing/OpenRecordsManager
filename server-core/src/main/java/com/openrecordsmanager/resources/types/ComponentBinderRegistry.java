package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.template.list.ListDefinition;
import com.openrecordsmanager.api.template.list.ListElementDefinition;
import com.openrecordsmanager.api.template.property.PropertyDefinition;
import com.openrecordsmanager.api.template.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.RecordType;

import java.util.Map;

public class ComponentBinderRegistry {
    public static final ComponentBinder<ListDefinition, ListType> LIST = new ListComponentBinder();
    public static final ComponentBinder<ListElementDefinition, ListElement> LIST_ELEMENT = new ListElementComponentBinder();
    public static final ComponentBinder<PropertyDefinition<?>, ObjectProperty<?>> PROPERTY = new ObjectPropertyComponentBinder();
    public static final ComponentBinder<RecordTypeDefinition, RecordType> RECORD_TYPE = new RecordTypeComponentBinder();

    private static final Map<ComponentType<?>, ComponentBinder<?, ?>> ENTRIES = Map.ofEntries(
            binding(ComponentTypes.LIST, LIST),
            binding(ComponentTypes.LIST_ELEMENT, LIST_ELEMENT),
            binding(ComponentTypes.PROPERTY, PROPERTY),
            binding(ComponentTypes.RECORD_TYPE, RECORD_TYPE)
    );

    @SuppressWarnings("unchecked")
    public static <T extends Component> ComponentBinder<T, ?> get(ComponentType<T> componentType) {
        ComponentBinder<T, ?> binder = (ComponentBinder<T, ?>) ENTRIES.get(componentType);
        if (binder == null) {
            throw new IllegalArgumentException("Component type " + componentType + " does not have an associated binder");
        }
        return binder;
    }

    private static <T extends Component> Map.Entry<ComponentType<T>, ComponentBinder<T, ?>> binding(
            ComponentType<T> componentType,
            ComponentBinder<T, ?> binding
    ) {
        return Map.entry(componentType, binding);
    }
}
