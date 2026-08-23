package com.openrecordsmanager.record;

import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import com.openrecordsmanager.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Entity
@Table(name = "record_property_value")
public class RecordPropertyValue<T> implements ObjectPropertyHolder.ObjectPropertyValue<T> {
    @Id
    public UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    public Record record;

    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    @JoinColumn(nullable = false)
    public ObjectProperty<T> property;

    @Column(name = "property_value")
    @JdbcTypeCode(SqlTypes.JSON)
    @Nullable
    public T value;

    @Deprecated
    protected RecordPropertyValue() {
    }

    public RecordPropertyValue(Record record, ObjectProperty<T> property, @Nullable T value) {
        this.id = UUID.randomUUID();
        this.record = record;
        this.property = property;
        this.value = value;
    }

    public boolean securityFilter(ExpressionsService expressions, User user, @Nullable Record record) {
        if (this.property.getSecurityFilter() == null) {
            return true;
        }

        return expressions.checkPropertyExpression(this.id, this.property.getSecurityFilter(), this.value, user, record);
    }

    @Override
    public ObjectProperty<T> getProperty() {
        return this.property;
    }

    @Override
    public @Nullable T getValue() {
        return this.value;
    }

    @Override
    public void setValue(@Nullable T value) {
        this.value = value;
    }

}
