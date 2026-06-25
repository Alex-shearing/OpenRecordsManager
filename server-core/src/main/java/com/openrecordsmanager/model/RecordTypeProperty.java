package com.openrecordsmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Embeddable
public class RecordTypeProperty<T> {

    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    public ObjectProperty<T> property;

    @Column()
    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    private T defaultValue;

    @Deprecated
    protected RecordTypeProperty() {
    }

    public RecordTypeProperty(ObjectProperty<T> property, @Nullable T defaultValue) {
        this.property = property;
        this.defaultValue = defaultValue;
    }

    /**
     * Get the default value for this property, if a specific default value is provided for this record type it will
     * be used, otherwise the default value for the record type will be provided.
     *
     * @return the default value for the property
     */
    @Nullable
    public T getDefault() {
        if (this.defaultValue != null) {
            return this.defaultValue;
        }

        return this.property.defaultValue;
    }

    public RecordPropertyValue<T> getPropertyValue(Record record, @Nullable T existingValue) {
        return new RecordPropertyValue<>(record, this.property, existingValue == null ? this.getDefault() : existingValue);
    }
}
