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

public class ComponentTypes {
    public static final ComponentType<ConfigDefinition<?>> CONFIG = ComponentType.of("config", ConfigDefinition.class);
    public static final ComponentType<InputAuthProviderType> INPUT_AUTH_PROVIDER = ComponentType.of("input_auth_provider", InputAuthProviderType.class);
    public static final ComponentType<RedirectAuthProviderType> REDIRECT_AUTH_PROVIDER = ComponentType.of("redirect_auth_provider", RedirectAuthProviderType.class);
    public static final ComponentType<FileStoreType<?>> FILE_STORE = ComponentType.of("file_store", FileStoreType.class);
    public static final ComponentType<FileStoreMiddlewareType<?>> FILE_STORE_MIDDLEWARE = ComponentType.of("file_store_middleware", FileStoreMiddlewareType.class);

    // Registerable components
    public static final ComponentType<ListDefinition> LIST = ComponentType.of("list", ListDefinition.class);
    public static final ComponentType<ListElementDefinition> LIST_ELEMENT = ComponentType.of("list_element", ListElementDefinition.class);
    public static final ComponentType<PropertyDefinition<?>> PROPERTY = ComponentType.of("object_property", PropertyDefinition.class);
    public static final ComponentType<RecordTypeDefinition> RECORD_TYPE = ComponentType.of("record_type", RecordTypeDefinition.class);

    private static final ComponentType<?>[] VALUES = {
            CONFIG,
            LIST,
            LIST_ELEMENT,
            PROPERTY,
            RECORD_TYPE,
            INPUT_AUTH_PROVIDER,
            REDIRECT_AUTH_PROVIDER,
            FILE_STORE,
            FILE_STORE_MIDDLEWARE
    };

    public static @Nullable ComponentType<?> fromName(String name) {
        for (ComponentType<?> value : VALUES) {
            if (Objects.equals(value.name, name)) return value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <K extends Component> ComponentType<K> fromObject(K object) {
        for (ComponentType<?> value : VALUES) {
            if (value.get(object).isPresent()) return (ComponentType<K>) value;
        }
        throw new IllegalArgumentException("Unable to get component type from object: " + object.getClass());
    }
}
