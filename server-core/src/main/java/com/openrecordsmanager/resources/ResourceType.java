package com.openrecordsmanager.resources;

import com.openrecordsmanager.RegisterableComponent;
import com.openrecordsmanager.auth.InputAuthProviderType;
import com.openrecordsmanager.auth.RedirectAuthProviderType;
import com.openrecordsmanager.config.ConfigDefinition;
import com.openrecordsmanager.list.ListDefinition;
import com.openrecordsmanager.property.PropertyDefinition;
import com.openrecordsmanager.recordtype.RecordTypeDefinition;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class ResourceType<T extends RegisterableComponent> {
    @SuppressWarnings("unchecked")
    public static final ResourceType<ConfigDefinition<?>> CONFIG = new ResourceType<>((Class<ConfigDefinition<?>>) (Class<?>) ConfigDefinition.class);
    public static final ResourceType<ListDefinition> LIST = new ResourceType<>(ListDefinition.class);
    @SuppressWarnings("unchecked")
    public static final ResourceType<PropertyDefinition<?>> PROPERTY = new ResourceType<>((Class<PropertyDefinition<?>>) (Class<?>) PropertyDefinition.class);
    public static final ResourceType<RecordTypeDefinition> RECORD_TYPE = new ResourceType<>(RecordTypeDefinition.class);
    public static final ResourceType<InputAuthProviderType> INPUT_AUTH_PROVIDER = new ResourceType<>(InputAuthProviderType.class);
    public static final ResourceType<RedirectAuthProviderType> REDIRECT_AUTH_PROVIDER = new ResourceType<>(RedirectAuthProviderType.class);

    private static final Map<String, ResourceType<?>> VALUES = Map.of(
            "config", CONFIG,
            "list", LIST,
            "property", PROPERTY,
            "record_type", RECORD_TYPE,
            "input_auth_provider", INPUT_AUTH_PROVIDER,
            "redirect_auth_provider", REDIRECT_AUTH_PROVIDER
    );

    private final Class<T> componentClass;

    private ResourceType(Class<T> componentClass) {
        this.componentClass = componentClass;
    }

    public <K extends RegisterableComponent> boolean is(K object) {
        return this.componentClass.isInstance(object);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <K extends RegisterableComponent> ResourceType<K> fromObject(K object) {
        for (ResourceType<?> value : VALUES.values()) {
            if (value.is(object)) {
                return (ResourceType<K>) value;
            }
        }

        return null;
    }

    @Nullable
    public static ResourceType<?> fromString(String id) {
        return VALUES.get(id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.componentClass.getSimpleName());
    }

    @Override
    public String toString() {
        return this.componentClass.getSimpleName();
    }
}
