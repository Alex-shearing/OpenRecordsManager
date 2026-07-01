package com.openrecordsmanager.model;

import com.openrecordsmanager.resources.ComponentCatalog;
import jakarta.persistence.*;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.InputStream;

@Entity
@Table(name = "plugin")
@JsonSerialize(using = Plugin.Serializer.class)
public class Plugin {
    @Id
    public String name;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    public FileStoreEntry file;

    @Deprecated
    protected Plugin() {
    }

    public Plugin(ComponentCatalog catalog, String name, FileStore<?> store, InputStream stream) {
        this.name = name;
        this.file = store.newFile(catalog, stream, "jar");
    }

    public static class Serializer extends ValueSerializer<Plugin> {
        @Override
        public void serialize(Plugin value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            gen.writeStringProperty("name", value.name);
            gen.writeStringProperty("hash", value.file.hash);
            gen.writeNumberProperty("size", value.file.sizeBytes);
            gen.writeEndObject();
        }
    }
}
