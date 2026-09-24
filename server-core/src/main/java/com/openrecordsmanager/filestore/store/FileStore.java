package com.openrecordsmanager.filestore.store;

import com.google.common.hash.HashFunction;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import com.openrecordsmanager.filestore.middleware.Middleware;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.hibernate.type.SqlTypes;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "file_store")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class FileStore {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @JavaType(ResourceIdentifierJavaType.class)
    private ResourceIdentifier type;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, ?> properties = new HashMap<>();

    @Column(nullable = false)
    private Instant dateCreated;

    @Column(nullable = false)
    private Instant dateModified;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "file_store_middleware_usage",
            joinColumns = @JoinColumn(name = "file_store_id")
    )
    @OrderBy("application_order ASC")
    private List<MiddlewareUsage> middlewares = new ArrayList<>();

    @Deprecated
    protected FileStore() {
    }

    public FileStore(ComponentCatalog catalog, String name, FileStoreType<?> type, Map<String, ?> properties) {
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.name = name;
        this.type = catalog.getRegistry(ComponentTypes.FILE_STORE).getId(type).orElseThrow();
        this.properties = JsonSchemaValidator.serializeSettings(type.parseSettings(properties));
        type.initializeUntyped(this.properties);
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.touchDateModified();
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
    }

    public void addMiddleware(Middleware middleware) {
        int index = this.middlewares.size();
        this.middlewares.add(new MiddlewareUsage(middleware, index));
        this.touchDateModified();
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
            path = this.getStoreType(catalog).saveUntyped(this.properties, stream, extension);
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
        return JsonSchemaValidator.serializeSettingsForClient(
                this.getStoreType(catalog).parseSettings(this.properties)
        );
    }

    public InputStream getFile(ComponentCatalog catalog, FileStoreEntry entry) throws IOException {
        InputStream stream = this.getStoreType(catalog).retrieveUntyped(this.properties, entry.getPath());

        for (MiddlewareUsage middleware : this.middlewares) {
            stream = middleware.middleware.duringRetrieve(catalog, stream);
        }

        return stream;
    }

    public FileStoreType<?> getStoreType(ComponentCatalog catalog) {
        return catalog.getRegistry(ComponentTypes.FILE_STORE).get(this.type).orElseThrow();
    }

    public void setProperties(ComponentCatalog catalog, Map<String, ?> properties) {
        FileStoreType<?> type = this.getStoreType(catalog);
        Map<String, Object> merged = JsonSchemaValidator.mergeFromExisting(
                properties,
                this.properties
        );
        this.properties = JsonSchemaValidator.serializeSettings(type.parseSettings(merged));
        type.initializeUntyped(this.properties);
        this.touchDateModified();
    }

    public List<Middleware> getMiddlewares() {
        return this.middlewares.stream().map(middlewareUsage -> middlewareUsage.middleware).toList();
    }

    @Embeddable
    private static class MiddlewareUsage {

        @ManyToOne(targetEntity = Middleware.class, optional = false)
        @JoinColumn(name = "middleware_id")
        private Middleware middleware;

        @SuppressWarnings("FieldCanBeLocal") // persisted; ordered via @OrderBy
        @Column(name = "application_order", nullable = false)
        private int applicationOrder;

        private MiddlewareUsage(Middleware middleware, int order) {
            this.middleware = middleware;
            this.applicationOrder = order;
        }

        @Deprecated
        protected MiddlewareUsage() {
        }
    }
}
