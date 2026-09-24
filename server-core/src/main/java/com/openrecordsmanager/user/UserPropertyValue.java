package com.openrecordsmanager.user;

import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

@Embeddable
@SuppressWarnings("NotNullFieldNotInitialized")
public class UserPropertyValue implements ObjectPropertyHolder.ObjectPropertyValue {
    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    @JoinColumn(name = "property_id", insertable = false, updatable = false)
    private ObjectProperty<?> property;

    @Column(name = "property_value")
    @JdbcTypeCode(SqlTypes.JSON)
    @Nullable
    private JsonNode value;

    @Deprecated
    protected UserPropertyValue() {
    }

    public UserPropertyValue(ObjectProperty<?> property, @Nullable JsonNode value) {
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
