package com.openrecordsmanager.recordtype;

import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

@Embeddable
@SuppressWarnings("NotNullFieldNotInitialized")
public class RecordTypeProperty<T> {

    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    private ObjectProperty<T> property;

    @Column()
    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode defaultValue;

    @Deprecated
    protected RecordTypeProperty() {
    }

    public RecordTypeProperty(ObjectProperty<T> property, @Nullable JsonNode defaultValue) {
        this.property = property;
        this.defaultValue = defaultValue;
    }

    public ObjectProperty<T> getProperty() {
        return this.property;
    }

    /**
     * Get the default value for this property as wire/storage JSON.
     * Record-type override wins over the object-property default.
     */
    @Nullable
    public JsonNode getDefault() {
        if (this.defaultValue != null) {
            return this.defaultValue;
        }

        return this.property.getDefaultValue();
    }

    /**
     * Apply this properties security filter to the provided record and actor
     *
     * @param expressions the expression service
     * @param record      the target record
     * @param actor       the target actor
     * @return true if the user can access otherwise false
     */
    public boolean securityFilter(ExpressionsService expressions, Record record, User actor) {
        if (this.property.getSecurityFilter() == null) {
            return true;
        }

        return expressions.checkPropertyExpression(
                record.getId(),
                this.property.getSecurityFilter(),
                record.getProperty(this.getProperty()),
                actor,
                record
        );
    }
}
