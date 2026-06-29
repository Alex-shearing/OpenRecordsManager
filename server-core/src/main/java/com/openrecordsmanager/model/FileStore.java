package com.openrecordsmanager.model;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.model.util.FileStoreTypeConverter;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.types.ComponentTypes;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Convert(converter = FileStoreTypeConverter.class)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    public FileStoreType<T> type;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> properties;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "store")
    public Set<FileStoreEntry> files;

    @Deprecated
    protected FileStore() {
    }

    public FileStore(FileStoreType<T> type, Map<String, Object> properties) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.properties = type.serialiseOptions(type.parseOptions(properties));
    }

    public FileStoreEntry newFile(InputStream file, String extension) {
        HashFunction hashFunction = getHashFunction(CURRENT_HASH_ALGORITHM);

        CountingInputStream countingStream = new CountingInputStream(file);
        HashingInputStream hashingStream = new HashingInputStream(hashFunction, countingStream);

        // Save the file into the store
        String path;
        try {
            path = this.type.save(this.getProperties(), hashingStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new FileStoreEntry(this, path, CURRENT_HASH_ALGORITHM, hashingStream.hash().toString(), countingStream.getCount(), extension);
    }

    public T getProperties() {
        return this.type.parseOptions(properties);
    }

    public InputStream getFile(FileStoreEntry entry) throws IOException {
        return this.type.retrieve(this.getProperties(), entry.path);
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = this.type.serialiseOptions(this.type.parseOptions(properties));
    }

    public static HashFunction getHashFunction(String algorithm) {
        return switch (algorithm) {
            case "SHA-256" -> Hashing.sha256();
            case "SHA-512" -> Hashing.sha512();
            default -> throw new IllegalStateException("Unknown hash function: " + algorithm);
        };
    }

    public static class Serializer extends ValueSerializer<FileStore<?>> {

        private final ComponentCatalog componentCatalog;

        @Autowired
        public Serializer(ComponentCatalog componentCatalog) {
            this.componentCatalog = componentCatalog;
        }

        @Override
        public void serialize(FileStore value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            gen.writeStringProperty("id", value.id.toString());
            gen.writeStringProperty("type", this.componentCatalog.getId(ComponentTypes.FILE_STORE_TYPE, value.type).toString());
            gen.writePOJOProperty("properties", value.properties);
            gen.writeEndObject();
        }
    }
}
