package com.openrecordsmanager.template.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;

@Schema(name = "TemplateType", enumAsRef = true)
public enum TemplateType {
    LIST(ComponentTypes.LIST),
    LIST_ELEMENT(ComponentTypes.LIST_ELEMENT),
    OBJECT_PROPERTY(ComponentTypes.OBJECT_PROPERTY),
    RECORD_TYPE(ComponentTypes.RECORD_TYPE),
    LOCATION_TYPE(ComponentTypes.LOCATION_TYPE),
    LOCATION_RELATIONSHIP_TYPE(ComponentTypes.LOCATION_RELATIONSHIP_TYPE);

    private final ComponentType<? extends TemplateComponent> componentType;

    TemplateType(ComponentType<? extends TemplateComponent> componentType) {
        this.componentType = componentType;
    }

    public ComponentType<? extends TemplateComponent> componentType() {
        return this.componentType;
    }

    @JsonValue
    public String key() {
        return this.componentType.name();
    }

    @JsonCreator
    public static TemplateType fromKey(String key) {
        return TemplateType.valueOf(key.toUpperCase(Locale.ROOT));
    }
}
