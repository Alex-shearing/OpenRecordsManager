package com.openrecordsmanager.user;

import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

@Entity
@Table(name = "user_property_value")
@SuppressWarnings("NotNullFieldNotInitialized")
public class UserPropertyValue implements ObjectPropertyHolder.ObjectPropertyValue {
    @Id
    public UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    public User user;

    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    @JoinColumn(nullable = false)
    public ObjectProperty<?> property;

    @Column(name = "property_value")
    @JdbcTypeCode(SqlTypes.JSON)
    @Nullable
    public JsonNode value;

    @Deprecated
    protected UserPropertyValue() {
    }

    public UserPropertyValue(User user, ObjectProperty<?> property, @Nullable JsonNode value) {
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.user = user;
        this.property = property;
        this.value = value;
    }

    @Override
    public ObjectProperty<?> getProperty() {
        return this.property;
    }

    @Override
    public @Nullable JsonNode getStoredValue() {
        return this.value;
    }

    @Override
    public void setStoredValue(@Nullable JsonNode value) {
        this.value = value;
    }
}
