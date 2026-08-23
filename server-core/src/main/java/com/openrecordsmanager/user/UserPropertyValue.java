package com.openrecordsmanager.user;

import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Entity
@Table(name = "user_property_value")
@SuppressWarnings("NotNullFieldNotInitialized")
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
    @Nullable
    public T value;

    @Deprecated
    protected UserPropertyValue() {
    }

    public UserPropertyValue(User user, ObjectProperty<T> property, @Nullable T value) {
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
    public @Nullable T getValue() {
        return this.value;
    }

    @Override
    public void setValue(@Nullable T value) {
        this.value = value;
    }
}
