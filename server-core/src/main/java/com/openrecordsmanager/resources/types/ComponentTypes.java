package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.api.list.ListElementDefinition;
import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.RecordType;
import org.jspecify.annotations.Nullable;

public class ComponentTypes {
    @SuppressWarnings("unchecked")
    public static final UnregisterableComponentType<ConfigDefinition<?>> CONFIG = new UnregisterableComponentType<>("configs", (Class<ConfigDefinition<?>>) (Class<?>) ConfigDefinition.class);
    public static final ComponentType<ListDefinition, ListType> LIST = new ListComponentType();
    public static final ComponentType<ListElementDefinition, ListElement> LIST_ELEMENT = new ListElementComponentType();
    public static final ComponentType<PropertyDefinition<?>, ObjectProperty<?>> PROPERTY = new ObjectPropertyComponentType();
    public static final ComponentType<RecordTypeDefinition, RecordType> RECORD_TYPE = new RecordTypeComponentType();
    public static final UnregisterableComponentType<InputAuthProviderType> INPUT_AUTH_PROVIDER = new UnregisterableComponentType<>("input_auth_providers", InputAuthProviderType.class);
    public static final UnregisterableComponentType<RedirectAuthProviderType> REDIRECT_AUTH_PROVIDER = new UnregisterableComponentType<>("redirect_auth_providers", RedirectAuthProviderType.class);

    public static final ComponentType<?, ?>[] VALUES = {
            CONFIG, LIST, LIST_ELEMENT, PROPERTY, RECORD_TYPE, INPUT_AUTH_PROVIDER, REDIRECT_AUTH_PROVIDER
    };

    @Nullable
    @SuppressWarnings("unchecked")
    public static <K extends Component, D> ComponentType<K, D> fromObject(K object) {
        for (ComponentType<?, ?> value : VALUES) {
            if (value.is(object)) {
                return (ComponentType<K, D>) value;
            }
        }

        return null;
    }
}
