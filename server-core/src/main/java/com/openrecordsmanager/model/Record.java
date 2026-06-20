package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.resources.ResourceIdentifier;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "record")
public class Record {
    @Id
    @JsonProperty
    public UUID id;

    @Column(nullable = false)
    @JsonProperty
    public String title;

    @ManyToOne
    @JoinColumn(name = "type_id")
    @JsonProperty
    public RecordType type;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "file_id")
    @JsonProperty
    public FileStoreEntry file;

    @OneToMany(cascade = CascadeType.ALL)
    @JsonProperty
    public Set<RecordPropertyValue<?>> properties;

    public Record() {
    }

    public Record(UUID id, String title, RecordType type, FileStoreEntry file, Set<RecordPropertyValue<?>> properties) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.file = file;
        this.properties = properties;
    }

    public <T> T getProperty(ObjectProperty<T> recordProperty) {
        return (T) this.getProperty(recordProperty.id.getId());
    }

    public Object getProperty(ResourceIdentifier id) {
        Optional<RecordPropertyValue<?>> property = this.properties.stream().filter(recordPropertyValue -> Objects.equals(recordPropertyValue.property.id.getId(), id)).findFirst();
        return property.map(userPropertyValue -> userPropertyValue.value).orElse(null);
    }
}
