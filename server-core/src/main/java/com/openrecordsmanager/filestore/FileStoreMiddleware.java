package com.openrecordsmanager.filestore;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.ComponentCatalog;
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
@JsonSerialize(using = FileStoreMiddleware.Serializer.class)
public class FileStoreMiddleware<T> {

    @Id
    public UUID id;

    @Column(nullable = false)
    public ResourceIdentifier type;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, ?> properties;

    @Deprecated
    protected FileStoreMiddleware() {
    }

    public FileStoreMiddleware(ComponentCatalog catalog, FileStoreMiddlewareType<T> type, Map<String, ?> properties) {
        this.id = UUID.randomUUID();
        this.type = catalog.getId(ComponentTypes.FILE_STORE_MIDDLEWARE, type).orElseThrow();
        this.properties = properties;
    }

    public T getProperties(ComponentCatalog catalog) {
        return this.getStoreType(catalog).parseOptions(this.properties);
    }

    @SuppressWarnings("unchecked")
    public FileStoreMiddlewareType<T> getStoreType(ComponentCatalog catalog) {
        return (FileStoreMiddlewareType<T>) catalog.getComponent(ComponentTypes.FILE_STORE_MIDDLEWARE, this.type).orElseThrow();
    }

    public InputStream duringSave(ComponentCatalog catalog, InputStream stream) {
        FileStoreMiddlewareType<T> type = this.getStoreType(catalog);
        return type.duringSave(this.getProperties(catalog), stream);
    }

    public InputStream duringRetrieve(ComponentCatalog catalog, InputStream stream) {
        FileStoreMiddlewareType<T> type = this.getStoreType(catalog);
        return type.duringRetrieve(this.getProperties(catalog), stream);
    }

    public void setProperties(Map<String, ?> properties) {
        this.properties = properties;
    }

    public static class Serializer extends ValueSerializer<FileStoreMiddleware<?>> {

        private final ComponentCatalog catalog;

        public Serializer(ComponentCatalog catalog) {
            this.catalog = catalog;
        }

        @Override
        public void serialize(FileStoreMiddleware value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            gen.writeStringProperty("id", value.id.toString());
            gen.writeStringProperty("type", value.type.toString());
            gen.writePOJOProperty("properties", value.getProperties(catalog));
            gen.writeEndObject();
        }
    }
}
