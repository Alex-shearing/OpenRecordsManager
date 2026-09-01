package com.openrecordsmanager.api.audit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import org.jspecify.annotations.Nullable;

/**
 * Identifies the kind of entity referenced by an audit event or relationship.
 * Serialized using the stable {@link #key()} form (e.g. {@code record_type}).
 */
public enum AuditEntityType {
    RECORD("record", null),
    USER("user", null),
    RECORD_TYPE("record_type", ComponentTypes.RECORD_TYPE),
    CONFIG("config", ComponentTypes.CONFIG),
    LIST("list", ComponentTypes.LIST),
    LIST_ELEMENT("list_element", ComponentTypes.LIST_ELEMENT),
    OBJECT_PROPERTY("object_property", ComponentTypes.OBJECT_PROPERTY),
    FILE_STORE("file_store", null),
    AUTH_PROVIDER("auth_provider", null),
    RECORD_REVISION("record_revision", null),
    FILE_STORE_MIDDLEWARE("file_store_middleware", null),
    TEMPLATE("template", null),
    PLUGIN("plugin", null);

    private final String key;
    private final @Nullable ComponentType<?> componentType;

    AuditEntityType(String key, @Nullable ComponentType<?> componentType) {
        this.key = key;
        this.componentType = componentType;
    }

    @JsonValue
    public String key() {
        return key;
    }

    @JsonCreator
    public static AuditEntityType fromKey(String key) {
        for (AuditEntityType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown audit entity type: " + key);
    }

    public static AuditEntityType fromComponentType(ComponentType<?> componentType) {
        for (AuditEntityType type : values()) {
            if (type.componentType != null && type.componentType.equals(componentType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported component type for audit:" + componentType.name);
    }
}
