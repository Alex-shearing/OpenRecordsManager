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

public class ResourceTypes {
    @SuppressWarnings("unchecked")
    public static final UnregisterableResourceType<ConfigDefinition<?>> CONFIG = new UnregisterableResourceType<>("config", (Class<ConfigDefinition<?>>) (Class<?>) ConfigDefinition.class);
    public static final ResourceType<ListDefinition, ListType> LIST = new ListResourceType();
    public static final ResourceType<ListElementDefinition, ListElement> LIST_ELEMENT = new ListElementResourceType();
    public static final ResourceType<PropertyDefinition<?>, ObjectProperty<?>> PROPERTY = new ObjectPropertyType();
    public static final ResourceType<RecordTypeDefinition, RecordType> RECORD_TYPE = new RecordTypeType();
    public static final UnregisterableResourceType<InputAuthProviderType> INPUT_AUTH_PROVIDER = new UnregisterableResourceType<>("input_auth_provider", InputAuthProviderType.class);
    public static final UnregisterableResourceType<RedirectAuthProviderType> REDIRECT_AUTH_PROVIDER = new UnregisterableResourceType<>("redirect_auth_provider", RedirectAuthProviderType.class);

    public static final ResourceType<?, ?>[] VALUES = {
            CONFIG, LIST, LIST_ELEMENT, PROPERTY, RECORD_TYPE, INPUT_AUTH_PROVIDER, REDIRECT_AUTH_PROVIDER
    };

    @Nullable
    @SuppressWarnings("unchecked")
    public static <K extends Component, D> ResourceType<K, D> fromObject(K object) {
        for (ResourceType<?, ?> value : VALUES) {
            if (value.is(object)) {
                return (ResourceType<K, D>) value;
            }
        }

        return null;
    }
}
