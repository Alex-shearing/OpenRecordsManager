package com.openrecordsmanager.filestore;

import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Entity
@Table(name = "file_store_entry")
public class FileStoreEntry {
    @Id
    public UUID id;

    @ManyToOne(optional = false)
    @JoinColumn
    public FileStore<?> store;

    @Column(nullable = false)
    public String path;

    @Column(nullable = false)
    public String hashAlgorithm;

    @Column(nullable = false)
    public String hash;

    @Column(nullable = false)
    public long sizeBytes;

    @Column()
    @Nullable
    public String extension;

    @Deprecated
    protected FileStoreEntry() {
    }

    public FileStoreEntry(FileStore<?> store, String path, String hashAlgorithm, String hash, long sizeBytes, @Nullable String extension) {
        this.id = UUID.randomUUID();
        this.store = store;
        this.path = path;
        this.hashAlgorithm = hashAlgorithm;
        this.hash = hash;
        this.sizeBytes = sizeBytes;
        this.extension = extension;
    }

    public InputStream getFile(ComponentCatalog catalog) {
        try {
            return this.store.getFile(catalog, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
