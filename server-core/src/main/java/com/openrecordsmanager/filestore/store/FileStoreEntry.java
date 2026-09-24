package com.openrecordsmanager.filestore.store;

import com.google.common.io.MoreFiles;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import jakarta.persistence.*;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.jspecify.annotations.Nullable;
import org.springframework.lang.Contract;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "file_store_entry")
@SuppressWarnings("NotNullFieldNotInitialized")
public class FileStoreEntry {
    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn
    private FileStore store;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String hashAlgorithm;

    @Column(nullable = false)
    private String hash;

    @Column(nullable = false)
    private long sizeBytes;

    @Column
    @Nullable
    private String extension;

    @Column(nullable = false)
    private Instant dateCreated;

    @Deprecated
    protected FileStoreEntry() {
    }

    public FileStoreEntry(FileStore store, String path, String hashAlgorithm, String hash, long sizeBytes, @Nullable String extension) {
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.store = store;
        this.path = path;
        this.hashAlgorithm = hashAlgorithm;
        this.hash = hash;
        this.sizeBytes = sizeBytes;
        this.extension = normalizeExtension(extension);
        this.dateCreated = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getPath() {
        return this.path;
    }

    public String getHash() {
        return this.hash;
    }

    public String getHashAlgorithm() {
        return this.hashAlgorithm;
    }

    public long getSizeBytes() {
        return this.sizeBytes;
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public @Nullable String getExtension() {
        return this.getExtension(null);
    }

    @Contract("!null -> !null")
    public @Nullable String getExtension(@Nullable String fallback) {
        return normalizeExtension(this.extension != null ? this.extension : fallback);
    }

    /**
     * Validates if the local path matches the stored file hash
     *
     * @param path the local file to compare
     * @return true if the file matches, false if different or the comparison failed
     */
    public boolean fileMatches(Path path) {
        try {
            String localHash = MoreFiles.asByteSource(path)
                    .hash(FileStoreService.getHashFunction(this.hashAlgorithm))
                    .toString();

            return localHash.equals(this.hash);
        } catch (IOException e) {
            return false;
        }
    }

    public InputStream getFile(ComponentCatalog catalog) {
        try {
            return this.store.getFile(catalog, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static @Nullable String normalizeExtension(@Nullable String extension) {
        if (extension == null || extension.isBlank()) {
            return null;
        }
        return extension.startsWith(".") ? extension.substring(1) : extension;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FileStoreEntry that = (FileStoreEntry) o;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
