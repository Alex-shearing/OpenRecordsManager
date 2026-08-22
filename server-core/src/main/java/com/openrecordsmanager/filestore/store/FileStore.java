package com.openrecordsmanager.filestore.store;

import com.google.common.hash.HashFunction;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.filestore.middleware.Middleware;
import com.openrecordsmanager.filestore.middleware.MiddlewareUsage;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Entity
@Table(name = "file_store")
@JsonSerialize(using = FileStore.Serializer.class)
public class FileStore {

    @Id
    public UUID id;

    @Column(nullable = false)
    public ResourceIdentifier type;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, ?> properties;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "file_store_middleware_usage",
            joinColumns = @JoinColumn(name = "file_store_id")
    )
    @OrderBy("application_order ASC")
    public List<MiddlewareUsage> middlewares = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "store")
    public Set<FileStoreEntry> files = new HashSet<>();

    @Deprecated
    protected FileStore() {
    }

    public FileStore(ComponentCatalog catalog, FileStoreType<?> type, Map<String, ?> properties) {
        this.id = UUID.randomUUID();
        this.type = catalog.getRegistry(ComponentTypes.FILE_STORE).getId(type).orElseThrow();
        this.properties = properties;
        this.middlewares = new ArrayList<>();
    }

    public void addMiddleware(Middleware middleware) {
        int index = this.middlewares.size();
        this.middlewares.add(new MiddlewareUsage(middleware, index));
    }

    public FileStoreEntry newFile(ComponentCatalog catalog, InputStream file, String extension) {
        HashFunction hashFunction = FileStoreService.getHashFunction(FileStoreService.CURRENT_HASH_ALGORITHM);

        CountingInputStream countingStream = new CountingInputStream(file);
        HashingInputStream hashingStream = new HashingInputStream(hashFunction, countingStream);

        InputStream stream = hashingStream;
        for (MiddlewareUsage middleware : this.middlewares) {
            stream = middleware.middleware.duringSave(catalog, stream);
        }

        // Save the stream into the store
        String path;
        try {
            path = this.getStoreType(catalog).saveUntyped(this.properties, stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new FileStoreEntry(
                this,
                path,
                FileStoreService.CURRENT_HASH_ALGORITHM,
                hashingStream.hash().toString(),
                countingStream.getCount(),
                extension
        );
    }

    public Object getProperties(ComponentCatalog catalog) {
        return this.getStoreType(catalog).parseSettings(this.properties);
    }

    public InputStream getFile(ComponentCatalog catalog, FileStoreEntry entry) throws IOException {
        InputStream stream = this.getStoreType(catalog).retrieveUntyped(this.properties, entry.path);

        for (MiddlewareUsage middleware : this.middlewares) {
            stream = middleware.middleware.duringRetrieve(catalog, stream);
        }

        return stream;
    }

    public FileStoreType<?> getStoreType(ComponentCatalog catalog) {
        return catalog.getRegistry(ComponentTypes.FILE_STORE).get(this.type).orElseThrow();
    }

    public void setProperties(Map<String, ?> properties) {
        this.properties = properties;
    }

    public static class Serializer extends ValueSerializer<FileStore> {

        private final ComponentCatalog catalog;

        public Serializer(ComponentCatalog catalog) {
            this.catalog = catalog;
        }

        @Override
        public void serialize(FileStore value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            gen.writeStringProperty("id", value.id.toString());
            gen.writeStringProperty("type", value.type.toString());
            gen.writePOJOProperty("properties", value.getProperties(catalog));
            gen.writePOJOProperty("middlewares", value.middlewares);
            gen.writeEndObject();
        }
    }
}
