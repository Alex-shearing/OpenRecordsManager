package com.openrecordsmanager.plugin;

import com.openrecordsmanager.filestore.store.FileStoreEntry;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "plugin")
@SuppressWarnings("NotNullFieldNotInitialized")
public class PersistedPlugin {
    @Id
    private String name;

    @Column(nullable = false)
    private String version;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn
    private @Nullable FileStoreEntry file;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column(nullable = false)
    private Instant dateModified;

    @Deprecated
    protected PersistedPlugin() {
    }

    public PersistedPlugin(String name, String version) {
        this.name = name;
        this.version = version;
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
        this.touchDateModified();
    }

    public @Nullable FileStoreEntry getFile() {
        return this.file;
    }

    public void setFile(@Nullable FileStoreEntry file) {
        this.file = file;
        this.touchDateModified();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
}
