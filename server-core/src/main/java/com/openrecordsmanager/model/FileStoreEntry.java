package com.openrecordsmanager.model;

import com.openrecordsmanager.resources.ComponentCatalog;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
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

    public FileStoreEntry(FileStore<?> store, String path, String hashAlgorithm, String hash, long sizeBytes) {
        this.id = UUID.randomUUID();
        this.store = store;
        this.path = path;
        this.hashAlgorithm = hashAlgorithm;
        this.hash = hash;
        this.sizeBytes = sizeBytes;
    }

    public Resource getFile(ComponentCatalog catalog) {
        try {
            return new InputStreamResource(this.store.getFile(this));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFileName(String name) {
        if (this.extension != null) {
            name += "." + this.extension;
        }
        return name;
    }
}
