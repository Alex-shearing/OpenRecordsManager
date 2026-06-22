package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.resources.ExpressionsService;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Entity
@Table(name = "record_property_value")
public class RecordPropertyValue<T> implements ObjectPropertyHolder.ObjectPropertyValue<T> {
    @Id
    @JsonProperty
    public UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    public Record record;

    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    @JoinColumn(nullable = false)
    public ObjectProperty<T> property;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty
    public T value;

    @Deprecated
    protected RecordPropertyValue() {
    }

    public RecordPropertyValue(Record record, ObjectProperty<T> property, T value) {
        this.id = UUID.randomUUID();
        this.record = record;
        this.property = property;
        this.value = value;
    }

    public boolean securityFilter(ExpressionsService expressions, User user, @Nullable Record record) {
        if (this.property == null) {
            return false;
        }
        if (this.property.securityFilter == null) {
            return true;
        }

        return expressions.checkPropertyExpression(this.id, this.property.securityFilter, this.value, user, record);
    }

    @Override
    public ObjectProperty<T> getProperty() {
        return this.property;
    }

    @Override
    public T getValue() {
        return this.value;
    }
}
