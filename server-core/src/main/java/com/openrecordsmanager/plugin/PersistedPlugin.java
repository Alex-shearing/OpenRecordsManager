package com.openrecordsmanager.plugin;

import com.openrecordsmanager.filestore.store.FileStoreEntry;
import jakarta.persistence.*;

@Entity
@Table(name = "plugin")
public class PersistedPlugin {
    @Id
    public String name;

    @Column(nullable = false)
    public String version;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    public FileStoreEntry file;

    @Deprecated
    protected PersistedPlugin() {
    }

    public PersistedPlugin(String name, String version) {
        this.name = name;
        this.version = version;
    }

}
