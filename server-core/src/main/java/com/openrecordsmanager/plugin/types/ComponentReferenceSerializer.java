package com.openrecordsmanager.plugin.types;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class ComponentReferenceSerializer extends ValueSerializer<ComponentReference<?>> {
    private final ComponentCatalog catalog;

    public ComponentReferenceSerializer(ComponentCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public void serialize(ComponentReference<?> value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("type", value.getType().name);
        gen.writeStringProperty("id", value.getId(catalog).orElseThrow().toString());
        gen.writeEndObject();
    }
}
