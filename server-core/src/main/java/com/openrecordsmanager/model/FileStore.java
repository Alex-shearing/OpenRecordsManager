package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import jakarta.persistence.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "file_store")
public class FileStore {
    private static final String CURRENT_HASH_ALGORITHM = "SHA-256";

    @Id
    @JsonProperty
    public UUID id;

    @Column(nullable = false)
    @JsonProperty
    public ResourceIdentifier type;

    @Column(nullable = false)
    @JsonProperty
    public Map<String, Object> properties;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "store")
    public Set<FileStoreEntry> files;

    @Deprecated
    protected FileStore() {
    }

    public FileStore(ResourceIdentifier type, Map<String, Object> properties) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.properties = properties;
    }

    public FileStoreEntry newFile(InputStream file, ComponentCatalog catalog) {
        FileStoreType type = catalog.getComponent(ComponentTypes.FILE_STORE_TYPE, this.type)
                .orElseThrow(() -> new IllegalArgumentException("file store type not found"));

        HashFunction hashFunction = getHashFunction(CURRENT_HASH_ALGORITHM);

        CountingInputStream countingStream = new CountingInputStream(file);
        HashingInputStream hashingStream = new HashingInputStream(hashFunction, countingStream);

        // Save the file into the store
        String path = "/test";
        try {
            type.save(this.properties, path, hashingStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new FileStoreEntry(this, path, CURRENT_HASH_ALGORITHM, hashingStream.hash().toString(), countingStream.getCount());
    }

    public InputStream getFile(FileStoreEntry entry, ComponentCatalog catalog) throws IOException {
        FileStoreType type = catalog.getComponent(ComponentTypes.FILE_STORE_TYPE, this.type)
                .orElseThrow(() -> new IllegalArgumentException("file store type not found"));
        return type.retrieve(this.properties, entry.path);
    }

    public static HashFunction getHashFunction(String algorithm) {
        return switch (algorithm) {
            case "SHA-256" -> Hashing.sha256();
            case "SHA-512" -> Hashing.sha512();
            default -> throw new IllegalStateException("Unknown hash function: " + algorithm);
        };
    }
}
