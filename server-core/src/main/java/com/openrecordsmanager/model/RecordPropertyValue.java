package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "record_property_value")
public class RecordPropertyValue<T> {
    @Id
    @JsonProperty
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "record_id")
    public Record record;

    @ManyToOne(targetEntity = ObjectProperty.class)
    @JoinColumn(name = "property_id")
    public ObjectProperty<T> property;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty
    public T value;

    public RecordPropertyValue() {
    }

    public RecordPropertyValue(Record record, ObjectProperty<T> property, T value) {
        this.id = UUID.randomUUID();
        this.record = record;
        this.property = property;
        this.value = value;
    }
}
