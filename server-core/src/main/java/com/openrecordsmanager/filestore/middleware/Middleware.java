package com.openrecordsmanager.filestore.middleware;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.schema.JsonSchemaValidator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.hibernate.type.SqlTypes;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

@Entity
@Table(name = "file_store_middleware")
@SuppressWarnings("NotNullFieldNotInitialized")
public class Middleware {

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

    @Deprecated
    protected Middleware() {
    }

    public Middleware(ComponentCatalog catalog, String name, FileStoreMiddlewareType<?> type, Map<String, ?> properties) {
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.name = name;
        this.type = catalog.getRegistry(ComponentTypes.FILE_STORE_MIDDLEWARE).getId(type).orElseThrow();
        this.properties = withParsedSettings(type, properties, (t, settings) -> {
            t.initialize(settings);
            return JsonSchemaValidator.serializeSettings(settings);
        });
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    public UUID getId() {
        return this.id;
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

    public Map<String, ?> getProperties(ComponentCatalog catalog) {
        return withParsedSettings(
                this.getMiddlewareType(catalog),
                this.properties,
                (_, settings) -> JsonSchemaValidator.serializeSettingsForClient(settings)
        );
    }

    public FileStoreMiddlewareType<?> getMiddlewareType(ComponentCatalog catalog) {
        return catalog.getRegistry(ComponentTypes.FILE_STORE_MIDDLEWARE)
                .get(this.type)
                .orElseThrow();
    }

    public InputStream duringSave(ComponentCatalog catalog, InputStream stream) {
        return withParsedSettings(
                this.getMiddlewareType(catalog),
                this.properties,
                (type, setting) -> type.duringSave(setting, stream)
        );
    }

    public InputStream duringRetrieve(ComponentCatalog catalog, InputStream stream) {
        return withParsedSettings(
                this.getMiddlewareType(catalog),
                this.properties,
                (type, setting) -> type.duringRetrieve(setting, stream)
        );
    }

    public void setProperties(ComponentCatalog catalog, Map<String, ?> properties) {
        this.properties = withParsedSettings(
                this.getMiddlewareType(catalog),
                JsonSchemaValidator.mergeFromExisting(properties, this.properties),
                (t, settings) -> {
                    t.initialize(settings);
                    return JsonSchemaValidator.serializeSettings(settings);
                }
        );
        this.touchDateModified();
    }

    private static <R, S extends Record> R withParsedSettings(
            FileStoreMiddlewareType<S> mw,
            Map<String, ?> settings,
            BiFunction<FileStoreMiddlewareType<S>, S, R> function
    ) {
        return function.apply(mw, JsonSchemaValidator.toRecord(mw.getSettingsClass(), settings));
    }
}
