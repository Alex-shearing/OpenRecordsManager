package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.api.list.ListElementDefinition;
import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.model.ListElement;
import com.openrecordsmanager.model.ListType;
import com.openrecordsmanager.model.ObjectProperty;
import com.openrecordsmanager.model.RecordType;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class ComponentTypes {
    public static final ComponentType<ConfigDefinition<?>> CONFIG = ComponentType.of("configs", ConfigDefinition.class);
    public static final ComponentType<InputAuthProviderType> INPUT_AUTH_PROVIDER = ComponentType.of("input_auth_providers", InputAuthProviderType.class);
    public static final ComponentType<RedirectAuthProviderType> REDIRECT_AUTH_PROVIDER = ComponentType.of("redirect_auth_providers", RedirectAuthProviderType.class);
    public static final ComponentType<FileStoreType<?>> FILE_STORE_TYPE = ComponentType.of("file_store_types", FileStoreType.class);

    public static final TemplateComponentType<ListDefinition, ListType> LIST = new ListComponentType("lists");
    public static final TemplateComponentType<ListElementDefinition, ListElement> LIST_ELEMENT = new ListElementComponentType("list_elements");
    public static final TemplateComponentType<PropertyDefinition<?>, ObjectProperty<?>> PROPERTY = new ObjectPropertyComponentType("object_properties");
    public static final TemplateComponentType<RecordTypeDefinition, RecordType> RECORD_TYPE = new RecordTypeComponentType("record_types");

    public static final ComponentType<?>[] VALUES = {
            CONFIG, LIST, LIST_ELEMENT, PROPERTY, RECORD_TYPE, INPUT_AUTH_PROVIDER, REDIRECT_AUTH_PROVIDER, FILE_STORE_TYPE
    };

    public static final TemplateComponentType<?, ?>[] REGISTERABLE_VALUES = {
            LIST, LIST_ELEMENT, PROPERTY, RECORD_TYPE
    };

    @Nullable
    public static ComponentType<?> fromName(String name) {
        for (ComponentType<?> value : VALUES) {
            if (Objects.equals(value.name, name)) return value;
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <K extends Component> ComponentType<K> fromObject(K object) {
        for (ComponentType<?> value : VALUES) {
            if (value.is(object)) {
                return (ComponentType<K>) value;
            }
        }

        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <K extends Component, D> TemplateComponentType<K, D> registerableFromObject(K object) {
        for (TemplateComponentType<?, ?> value : REGISTERABLE_VALUES) {
            if (value.is(object)) {
                return (TemplateComponentType<K, D>) value;
            }
        }

        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static TemplateComponentType<Component, ?> registerableFromName(String name) {
        for (TemplateComponentType<? extends Component, ?> value : REGISTERABLE_VALUES) {
            if (Objects.equals(value.name, name)) return (TemplateComponentType<Component, ?>) value;
        }

        return null;
    }
}
