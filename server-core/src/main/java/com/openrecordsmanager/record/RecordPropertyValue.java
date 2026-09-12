package com.openrecordsmanager.record;

import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import com.openrecordsmanager.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

@Entity
@Table(name = "record_property_value")
@SuppressWarnings("NotNullFieldNotInitialized")
public class RecordPropertyValue implements ObjectPropertyHolder.ObjectPropertyValue {
    @Id
    public UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    public Record record;

    @ManyToOne(targetEntity = ObjectProperty.class, optional = false)
    @JoinColumn(nullable = false)
    public ObjectProperty<?> property;

    @Column(name = "property_value")
    @JdbcTypeCode(SqlTypes.JSON)
    @Nullable
    public JsonNode value;

    @Deprecated
    protected RecordPropertyValue() {
    }

    public RecordPropertyValue(Record record, ObjectProperty<?> property, @Nullable JsonNode value) {
        this.id = UUID.randomUUID();
        this.record = record;
        this.property = property;
        this.value = value;
    }

    public boolean securityFilter(ExpressionsService expressions, User actor, @Nullable Record record) {
        if (this.property.getSecurityFilter() == null) {
            return true;
        }

        Object domain = this.record.getProperty(this.property);
        return expressions.checkPropertyExpression(this.id, this.property.getSecurityFilter(), domain, actor, record);
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
