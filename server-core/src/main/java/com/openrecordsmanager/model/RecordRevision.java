package com.openrecordsmanager.model;

import jakarta.persistence.*;

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
public class RecordRevision {
    @Id
    public UUID id;

    @Column(nullable = false)
    public double version;

    @Column(nullable = false)
    public Instant createdDate;

    @ManyToOne
    @JoinColumn(nullable = false, name = "record_id")
    public Record record;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    public FileStoreEntry file;

    @Deprecated
    protected RecordRevision() {
    }

    public RecordRevision(double version, Record record, FileStoreEntry file) {
        this.id = UUID.randomUUID();
        this.version = version;
        this.record = record;
        this.file = file;
        this.createdDate = Instant.now();
    }

}
