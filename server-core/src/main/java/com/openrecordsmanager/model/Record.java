package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.resources.ExpressionsService;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private RecordType type;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn
    @JsonProperty
    public FileStoreEntry file;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "record")
    @MapKey(name = "property")
    @JsonProperty
    public Map<ObjectProperty<?>, RecordPropertyValue<?>> properties;

    @Deprecated
    protected Record() {
    }

    public Record(UUID id, String title, RecordType type, FileStoreEntry file) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.file = file;
        if (this.type != null) {
            this.properties = type.properties.stream()
                    .map(prop -> Map.entry(prop.property, prop.getPropertyValue(this, null)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            this.properties = new HashMap<>();
        }
    }

    public RecordType getType() {
        return this.type;
    }

    public void setType(RecordType type) {
        this.type = type;
        this.properties = type.properties.stream()
                .map(prop -> Map.entry(prop.property, newProperty(prop)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public <V> RecordPropertyValue<V> createProperty(ObjectProperty<V> property, V value) {
        return new RecordPropertyValue<>(this, property, value);
    }

    private <T> RecordPropertyValue<T> newProperty(RecordTypeProperty<T> property) {
        return this.createProperty(property.property, property.getDefault());
    }

    @Override
    public boolean hasProperty(ObjectProperty<?> property) {
        return this.type == null || this.properties.containsKey(property);
    }

    @Override
    public Map<ObjectProperty<?>, RecordPropertyValue<?>> getProperties() {
        return this.properties;
    }

    public SecurityFilterUsage securityFilter(ExpressionsService expressions, User user, @Nullable Record record) {
        // Check record type filter
        if (!expressions.checkPropertyExpression(this.id, this.type.securityFilter, null, user, record)) {
            return this.type.securityFilterUsage;
        }

        // Check all properties
        for (RecordPropertyValue<?> property : this.properties.values()) {
            if (!property.securityFilter(expressions, user, record)) {
                return this.type.securityFilterUsage;
            }
        }

        return SecurityFilterUsage.SHOW_ALL;
    }
}
