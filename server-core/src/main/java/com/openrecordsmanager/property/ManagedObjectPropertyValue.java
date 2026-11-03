package com.openrecordsmanager.property;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Embeddable
@SuppressWarnings("NotNullFieldNotInitialized")
public class ManagedObjectPropertyValue implements ManagedObject.ObjectPropertyValue {
    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    @JoinColumn(name = "property_id", insertable = false, updatable = false)
    private ObjectProperty<?> property;

    @Column(name = "property_value", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode value;

    @Deprecated
    protected ManagedObjectPropertyValue() {
    }

    public ManagedObjectPropertyValue(ObjectProperty<?> property, JsonNode value) {
        this.property = property;
        this.value = value;
    }

    @Override
    public ObjectProperty<?> getProperty() {
        return this.property;
    }

    @Override
    public JsonNode getStoredValue() {
        return this.value;
    }

    @Override
    public void setStoredValue(JsonNode value) {
        this.value = value;
    }
}
