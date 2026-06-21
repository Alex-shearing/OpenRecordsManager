package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.resources.ResourceIdentifier;
import jakarta.persistence.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "file_store")
public class FileStore {
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
}
