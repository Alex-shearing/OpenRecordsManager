package com.openrecordsmanager.filestore.store;

import com.google.common.hash.HashFunction;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.filestore.middleware.Middleware;
import com.openrecordsmanager.filestore.middleware.MiddlewareUsage;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Entity
@Table(name = "file_store")
public class FileStore {

    @Id
    private UUID id;

    @Column(nullable = false)
    private ResourceIdentifier type;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, ?> properties;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "file_store_middleware_usage",
            joinColumns = @JoinColumn(name = "file_store_id")
    )
    @OrderBy("application_order ASC")
    private List<MiddlewareUsage> middlewares = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "store")
    private Set<FileStoreEntry> files = new HashSet<>();

    @Deprecated
    protected FileStore() {
    }

    public FileStore(ComponentCatalog catalog, FileStoreType<?> type, Map<String, ?> properties) {
        this.id = UUID.randomUUID();
        this.type = catalog.getRegistry(ComponentTypes.FILE_STORE).getId(type).orElseThrow();
        this.properties = JsonSchemaValidator.serializeSettings(type.parseSettings(properties));
        this.middlewares = new ArrayList<>();
    }

    public UUID getId() {
        return id;
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

    public Map<String, ?> getProperties(ComponentCatalog catalog) {
        return JsonSchemaValidator.serializeSettings(this.getStoreType(catalog).parseSettings(this.properties));
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

    public void setProperties(ComponentCatalog catalog, Map<String, ?> properties) {
        this.properties = JsonSchemaValidator.serializeSettings(this.getStoreType(catalog).parseSettings(properties));
    }

    public List<MiddlewareUsage> getMiddlewares() {
        return middlewares;
    }

    public Set<FileStoreEntry> getFiles() {
        return files;
    }
}
