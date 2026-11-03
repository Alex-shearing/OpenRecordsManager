package com.openrecordsmanager.template;

import com.openrecordsmanager.property.ObjectProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

@Embeddable
@SuppressWarnings("NotNullFieldNotInitialized")
public class ManagedObjectTypeProperty<T> {

    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    private ObjectProperty<T> property;

    @Column(name = "default_value")
    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode defaultValue;

    @Deprecated
    protected ManagedObjectTypeProperty() {
    }

    public ManagedObjectTypeProperty(ObjectProperty<T> property, @Nullable JsonNode defaultValue) {
        this.property = property;
        this.defaultValue = defaultValue;
    }

    public ObjectProperty<T> getProperty() {
        return this.property;
    }

    /**
     * Type-level override wins over the object-property default.
     */
    @Nullable
    public JsonNode getDefault() {
        if (this.defaultValue != null) {
            return this.defaultValue;
        }

        return this.property.getDefaultValue();
    }

    /**
     * Identity is the bound property: a type may declare each property at most once.
     * Required so Hibernate {@code Set} element collections do not inflate under join fetches.
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ManagedObjectTypeProperty<?> that)) {
            return false;
        }
        return Objects.equals(this.property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.property);
    }
}
