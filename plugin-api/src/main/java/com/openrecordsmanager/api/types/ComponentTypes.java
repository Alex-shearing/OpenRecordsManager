package com.openrecordsmanager.api.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.template.list.ListDefinition;
import com.openrecordsmanager.api.template.list.ListElementDefinition;
import com.openrecordsmanager.api.template.property.PropertyDefinition;
import com.openrecordsmanager.api.template.recordtype.RecordTypeDefinition;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public interface ComponentTypes {
    ComponentType<ConfigDefinition<?>> CONFIG = ComponentType.of("config", ConfigDefinition.class);
    ComponentType<InputAuthProviderType> INPUT_AUTH_PROVIDER = ComponentType.of("input_auth_provider", InputAuthProviderType.class);
    ComponentType<RedirectAuthProviderType> REDIRECT_AUTH_PROVIDER = ComponentType.of("redirect_auth_provider", RedirectAuthProviderType.class);
    ComponentType<FileStoreType<?>> FILE_STORE_TYPE = ComponentType.of("file_store_type", FileStoreType.class);
    ComponentType<FileStoreMiddlewareType<?>> FILE_STORE_MIDDLEWARE = ComponentType.of("file_store_middleware", FileStoreMiddlewareType.class);

    // Registerable components
    ComponentType<ListDefinition> LIST = ComponentType.of("list", ListDefinition.class);
    ComponentType<ListElementDefinition> LIST_ELEMENT = ComponentType.of("list_element", ListElementDefinition.class);
    ComponentType<PropertyDefinition<?>> PROPERTY = ComponentType.of("object_property", PropertyDefinition.class);
    ComponentType<RecordTypeDefinition> RECORD_TYPE = ComponentType.of("record_type", RecordTypeDefinition.class);

    ComponentType<?>[] VALUES = {
            CONFIG,
            LIST,
            LIST_ELEMENT,
            PROPERTY,
            RECORD_TYPE,
            INPUT_AUTH_PROVIDER,
            REDIRECT_AUTH_PROVIDER,
            FILE_STORE_TYPE,
            FILE_STORE_MIDDLEWARE
    };

    @SuppressWarnings("unchecked")
    static @Nullable ComponentType<Component> fromName(String name) {
        for (ComponentType<?> value : VALUES) {
            if (Objects.equals(value.name, name)) return (ComponentType<Component>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static <K extends Component> ComponentType<K> fromObject(K object) {
        for (ComponentType<?> value : VALUES) {
            if (value.is(object)) return (ComponentType<K>) value;
        }
        throw new IllegalArgumentException("Unable to get component type from object: " + object.getClass());
    }
}
