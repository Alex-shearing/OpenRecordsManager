package com.openrecordsmanager.record;

import com.openrecordsmanager.filestore.store.FileStoreEntry;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "record_revision",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_record_version",
                        columnNames = {"record_id", "version"}
                )
        }
)
@SuppressWarnings("NotNullFieldNotInitialized")
public class RecordRevision {
    @Id
    private UUID id;

    @Column(nullable = false)
    @Pattern(
            regexp = "^[0-9.]+$",
            message = "Version must only contain numeric digits and decimals"
    )
    private String version;

    @Column(nullable = false)
    private Instant createdDate;

    @ManyToOne
    @JoinColumn(nullable = false, name = "record_id")
    private Record record;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    private FileStoreEntry file;

    @Deprecated
    protected RecordRevision() {
    }

    public RecordRevision(String version, Record record, FileStoreEntry file) {
        this.id = UUID.randomUUID();
        this.version = version;
        this.record = record;
        this.file = file;
        this.createdDate = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getVersion() {
        return this.version;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public Record getRecord() {
        return this.record;
    }

    public FileStoreEntry getFile() {
        return this.file;
    }
}
