package com.openrecordsmanager.record;

import com.openrecordsmanager.filestore.store.FileStoreEntry;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import org.hibernate.id.uuid.UuidVersion7Strategy;

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
    private Instant dateCreated;

    @ManyToOne
    @JoinColumn(nullable = false, name = "record_id")
    private Record record;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(nullable = false)
    private FileStoreEntry file;

    @Deprecated
    protected RecordRevision() {
    }

    public RecordRevision(String version, Record record, FileStoreEntry file) {
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.version = version;
        this.record = record;
        this.file = file;
        this.dateCreated = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getVersion() {
        return this.version;
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public Record getRecord() {
        return this.record;
    }

    public FileStoreEntry getFile() {
        return this.file;
    }
}
