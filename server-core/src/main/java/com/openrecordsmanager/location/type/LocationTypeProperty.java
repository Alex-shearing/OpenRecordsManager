package com.openrecordsmanager.location.type;

import com.openrecordsmanager.property.ObjectProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

@Embeddable
@SuppressWarnings("NotNullFieldNotInitialized")
public class LocationTypeProperty<T> {

    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    private ObjectProperty<T> property;

    @Column()
    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode defaultValue;

    @Deprecated
    protected LocationTypeProperty() {
    }

    public LocationTypeProperty(ObjectProperty<T> property, @Nullable JsonNode defaultValue) {
        this.property = property;
        this.defaultValue = defaultValue;
    }

    public ObjectProperty<T> getProperty() {
        return this.property;
    }

    /**
     * Get the default value for this property as wire/storage JSON.
     * Location-type override wins over the object-property default.
     */
    @Nullable
    public JsonNode getDefault() {
        if (this.defaultValue != null) {
            return this.defaultValue;
        }

        return this.property.getDefaultValue();
    }
}
