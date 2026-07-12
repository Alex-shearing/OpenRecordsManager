package com.openrecordsmanager.plugin;

import com.openrecordsmanager.filestore.FileStoreEntry;
import jakarta.persistence.*;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

@Entity
@Table(name = "plugin")
@JsonSerialize(using = PersistedPlugin.Serializer.class)
public class PersistedPlugin {
    @Id
    public String name;

    @Column(nullable = false)
    public String version;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    public FileStoreEntry file;

    @Deprecated
    protected PersistedPlugin() {
    }

    public PersistedPlugin(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public static class Serializer extends ValueSerializer<PersistedPlugin> {
        @Override
        public void serialize(PersistedPlugin value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            gen.writeStringProperty("name", value.name);
            gen.writeStringProperty("hash", value.file.hash);
            gen.writeNumberProperty("size", value.file.sizeBytes);
            gen.writeEndObject();
        }
    }
}
