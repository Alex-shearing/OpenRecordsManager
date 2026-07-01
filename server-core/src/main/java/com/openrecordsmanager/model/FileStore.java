package com.openrecordsmanager.model;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "file_store")
@JsonSerialize(using = FileStore.Serializer.class)
public class FileStore<T> {
    private static final String CURRENT_HASH_ALGORITHM = "SHA-256";

    @Id
    public UUID id;

    @Column(nullable = false)
    public ResourceIdentifier type;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> properties;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "store")
    public Set<FileStoreEntry> files;

    @Deprecated
    protected FileStore() {
    }

    public FileStore(ComponentCatalog catalog, FileStoreType<T> type, Map<String, Object> properties) {
        this.id = UUID.randomUUID();
        this.type = catalog.getId(ComponentTypes.FILE_STORE_TYPE, type);
        this.properties = type.serialiseOptions(type.parseOptions(properties));
    }

    public FileStoreEntry newFile(ComponentCatalog catalog, InputStream file, String extension) {
        HashFunction hashFunction = getHashFunction(CURRENT_HASH_ALGORITHM);

        CountingInputStream countingStream = new CountingInputStream(file);
        HashingInputStream hashingStream = new HashingInputStream(hashFunction, countingStream);

        // Save the file into the store
        String path;
        try {
            path = this.getStoreType(catalog).save(this.getProperties(catalog), hashingStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new FileStoreEntry(
                this,
                path,
                CURRENT_HASH_ALGORITHM,
                hashingStream.hash().toString(),
                countingStream.getCount(),
                extension
        );
    }

    public T getProperties(ComponentCatalog catalog) {
        return this.getStoreType(catalog).parseOptions(properties);
    }

    public InputStream getFile(ComponentCatalog catalog, FileStoreEntry entry) throws IOException {
        return this.getStoreType(catalog).retrieve(this.getProperties(catalog), entry.path);
    }

    public FileStoreType<T> getStoreType(ComponentCatalog catalog) {
        return (FileStoreType<T>) catalog.getComponent(ComponentTypes.FILE_STORE_TYPE, this.type).orElseThrow();
    }

    public void setProperties(ComponentCatalog catalog, Map<String, Object> properties) {
        FileStoreType<T> type = this.getStoreType(catalog);
        this.properties = type.serialiseOptions(type.parseOptions(properties));
    }

    public static HashFunction getHashFunction(String algorithm) {
        return switch (algorithm) {
            case "SHA-256" -> Hashing.sha256();
            case "SHA-512" -> Hashing.sha512();
            default -> throw new IllegalStateException("Unknown hash function: " + algorithm);
        };
    }

    public static class Serializer extends ValueSerializer<FileStore<?>> {

        @Override
        public void serialize(FileStore value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            gen.writeStringProperty("id", value.id.toString());
            gen.writeStringProperty("type", value.type.toString());
            gen.writePOJOProperty("properties", value.properties);
            gen.writeEndObject();
        }
    }
}
