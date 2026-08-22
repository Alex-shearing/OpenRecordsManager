package com.openrecordsmanager.filestore.middleware;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "file_store_middleware")
@JsonSerialize(using = Middleware.Serializer.class)
public class Middleware {

    @Id
    public UUID id;

    @Column(nullable = false)
    public ResourceIdentifier type;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, ?> properties;

    @Deprecated
    protected Middleware() {
    }

    public Middleware(ComponentCatalog catalog, FileStoreMiddlewareType<?> type, Map<String, ?> properties) {
        this.id = UUID.randomUUID();
        this.type = catalog.getRegistry(ComponentTypes.FILE_STORE_MIDDLEWARE).getId(type).orElseThrow();
        this.properties = properties;
    }

    public Object getProperties(ComponentCatalog catalog) {
        return this.getMiddlewareType(catalog).parseSettings(this.properties);
    }

    public FileStoreMiddlewareType<?> getMiddlewareType(ComponentCatalog catalog) {
        return catalog.getRegistry(ComponentTypes.FILE_STORE_MIDDLEWARE)
                .get(this.type)
                .orElseThrow();
    }

    public InputStream duringSave(ComponentCatalog catalog, InputStream stream) {
        return this.getMiddlewareType(catalog).duringSaveUntyped(this.properties, stream);
    }

    public InputStream duringRetrieve(ComponentCatalog catalog, InputStream stream) {
        return this.getMiddlewareType(catalog).duringRetrieveUntyped(this.properties, stream);
    }

    public void setProperties(Map<String, ?> properties) {
        this.properties = properties;
    }

    public static class Serializer extends ValueSerializer<Middleware> {

        private final ComponentCatalog catalog;

        public Serializer(ComponentCatalog catalog) {
            this.catalog = catalog;
        }

        @Override
        public void serialize(Middleware value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            gen.writeStringProperty("id", value.id.toString());
            gen.writeStringProperty("type", value.type.toString());
            gen.writePOJOProperty("properties", value.getProperties(catalog));
            gen.writeEndObject();
        }
    }
}
