package com.openrecordsmanager.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class RecordTypeProperty<T> {

    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    public ObjectProperty<?> property;

    @JdbcTypeCode(SqlTypes.JSON)
    public T defaultValue;

    @Deprecated
    public RecordTypeProperty() {
    }

    public RecordTypeProperty(ObjectProperty<T> property, T defaultValue) {
        this.property = property;
        this.defaultValue = defaultValue;
    }
}
