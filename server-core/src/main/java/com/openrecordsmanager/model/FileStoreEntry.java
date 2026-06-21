package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "file_store_entry")
public class FileStoreEntry {
    @Id
    @JsonProperty
    public UUID id;

    @ManyToOne(optional = false)
    @JoinColumn
    @JsonProperty
    public FileStore store;

    @Column(nullable = false)
    @JsonProperty
    public String path;

    @Column(nullable = false)
    @JsonProperty
    public String hashAlgorithm;

    @Column(nullable = false)
    @JsonProperty
    public String hash;

    @Column(nullable = false)
    @JsonProperty
    public long sizeBytes;

    @Deprecated
    protected FileStoreEntry() {
    }
}
