package com.openrecordsmanager.model;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.model.util.FileStoreTypeConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
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
    public ObjectNode properties;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "store")
    public Set<FileStoreEntry> files;

    @Deprecated
    protected FileStore() {
    }

    public FileStore(FileStoreType<T> type, ObjectNode properties) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.properties = properties;
    }

    public FileStoreEntry newFile(InputStream file) {
        HashFunction hashFunction = getHashFunction(CURRENT_HASH_ALGORITHM);

        CountingInputStream countingStream = new CountingInputStream(file);
        HashingInputStream hashingStream = new HashingInputStream(hashFunction, countingStream);

        // Save the file into the store
        String path = "/test";
        try {
            type.save(type.parseOptions(this.properties), path, hashingStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new FileStoreEntry(this, path, CURRENT_HASH_ALGORITHM, hashingStream.hash().toString(), countingStream.getCount());
    }

    public InputStream getFile(FileStoreEntry entry) throws IOException {
        return type.retrieve(type.parseOptions(this.properties), entry.path);
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
