package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.resources.ExpressionsService;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "record")
public class Record implements ObjectPropertyHolder<RecordPropertyValue<?>> {
    @Id
    @JsonProperty
    public UUID id;

    @Column(nullable = false)
    @JsonProperty
    public String title;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "FK_RECORD_RECORDTYPE"))
    @JsonProperty
    public RecordType type;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn
    @JsonProperty
    public FileStoreEntry file;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "record")
    @JsonProperty
    public Set<RecordPropertyValue<?>> properties;

    @Deprecated
    protected Record() {
    }

    public Record(UUID id, String title, RecordType type, FileStoreEntry file) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.file = file;
        this.properties = new HashSet<>();
    }

    @Override
    public Set<RecordPropertyValue<?>> getProperties() {
        return this.properties;
    }

    public SecurityFilterUsage securityFilter(ExpressionsService expressions, User user, @Nullable Record record) {
        // Check record type filter
        if (!expressions.checkPropertyExpression(this.id, this.type.securityFilter, null, user, record)) {
            return this.type.securityFilterUsage;
        }

        // Check all properties
        for (RecordPropertyValue<?> property : this.properties) {
            if (!property.securityFilter(expressions, user, record)) {
                return this.type.securityFilterUsage;
            }
        }

        return SecurityFilterUsage.SHOW_ALL;
    }
}
