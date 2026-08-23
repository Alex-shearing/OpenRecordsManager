package com.openrecordsmanager.api.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.action.RecordActionType;
import com.openrecordsmanager.api.action.UserActionType;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.template.list.ListElementTemplate;
import com.openrecordsmanager.api.template.list.ListTemplate;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.recordtype.RecordTypeTemplate;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public class ComponentTypes {
    public static final ComponentType<ConfigType<?>> CONFIG = ComponentType.of("config", ConfigType.class);
    public static final ComponentType<InputAuthProviderType<?>> INPUT_AUTH_PROVIDER = ComponentType.of("input_auth_provider", InputAuthProviderType.class);
    public static final ComponentType<RedirectAuthProviderType> REDIRECT_AUTH_PROVIDER = ComponentType.of("redirect_auth_provider", RedirectAuthProviderType.class);
    public static final ComponentType<FileStoreType<?>> FILE_STORE = ComponentType.of("file_store", FileStoreType.class);
    public static final ComponentType<FileStoreMiddlewareType<?>> FILE_STORE_MIDDLEWARE = ComponentType.of("file_store_middleware", FileStoreMiddlewareType.class);
    public static final ComponentType<UserActionType<?>> USER_ACTION = ComponentType.of("user_action", UserActionType.class);
    public static final ComponentType<RecordActionType<?>> RECORD_ACTION = ComponentType.of("record_action", RecordActionType.class);

    // Registerable components
    public static final ComponentType<ListTemplate> LIST = ComponentType.of("list", ListTemplate.class);
    public static final ComponentType<ListElementTemplate> LIST_ELEMENT = ComponentType.of("list_element", ListElementTemplate.class);
    public static final ComponentType<ObjectPropertyTemplate<?>> OBJECT_PROPERTY = ComponentType.of("object_property", ObjectPropertyTemplate.class);
    public static final ComponentType<RecordTypeTemplate> RECORD_TYPE = ComponentType.of("record_type", RecordTypeTemplate.class);

    private static final Set<ComponentType<?>> VALUES = Set.of(
            CONFIG,
            LIST,
            LIST_ELEMENT,
            OBJECT_PROPERTY,
            RECORD_TYPE,
            INPUT_AUTH_PROVIDER,
            REDIRECT_AUTH_PROVIDER,
            FILE_STORE,
            FILE_STORE_MIDDLEWARE,
            USER_ACTION,
            RECORD_ACTION
    );

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
