package com.openrecordsmanager.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "user_property_value")
public class UserPropertyValue<T> implements ObjectPropertyHolder.ObjectPropertyValue<T> {
    @Id
    public UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    public User user;

    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    @JoinColumn(nullable = false)
    public ObjectProperty<T> property;

    @Column(name = "property_value")
    @JdbcTypeCode(SqlTypes.JSON)
    public T value;

    @Deprecated
    protected UserPropertyValue() {
    }

    public UserPropertyValue(User user, ObjectProperty<T> property, T value) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.property = property;
        this.value = value;
    }

    @Override
    public ObjectProperty<T> getProperty() {
        return this.property;
    }

    @Override
    public T getValue() {
        return this.value;
    }

    @Override
    public void setValue(T value) {
        this.value = value;
    }
}
