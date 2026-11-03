package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.util.PropertyTypeConverter;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.template.RegisteredComponent;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "object_property")
@DiscriminatorValue("object_property")
@PrimaryKeyJoinColumn(name = "id")
public class ObjectProperty<T> extends RegisteredComponent {
    @Column(nullable = false)
    @Convert(converter = PropertyTypeConverter.class)
    @JavaType(ObjectJavaType.class)
    private PropertyType<T> type;

    @ManyToOne
    @JoinColumn
    @Nullable
    private ListType listType;

    @Column()
    @Nullable
    private String validator;

    @Column()
    @Nullable
    private String securityFilter;

    @Column()
    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode defaultValue;

    @Column(nullable = false)
    private boolean systemProperty;

    @Deprecated
    protected ObjectProperty() {
    }

    public ObjectProperty(
            ResourceIdentifier identifier,
            PropertyType<T> type,
            @Nullable ListType listType,
            @Nullable String validator,
            @Nullable String securityFilter,
            @Nullable JsonNode defaultValue,
            boolean systemProperty
    ) {
        super(identifier);
        this.type = type;
        this.listType = listType;
        this.validator = validator;
        this.securityFilter = securityFilter;
        this.defaultValue = defaultValue;
        this.systemProperty = systemProperty;
    }

    public ObjectProperty(ResourceIdentifier identifier, PropertyType<T> type) {
        this(identifier, type, null, null, null, null, false);
    }

    public PropertyType<T> getType() {
        return this.type;
    }

    public @Nullable ListType getListType() {
        return this.listType;
    }

    public @Nullable String getValidator() {
        return this.validator;
    }

    public void setValidator(@Nullable String validator) {
        this.validator = validator;
        this.touchDateModified();
    }

    public @Nullable String getSecurityFilter() {
        return this.securityFilter;
    }

    public void setSecurityFilter(@Nullable String securityFilter) {
        this.securityFilter = securityFilter;
        this.touchDateModified();
    }

    public @Nullable JsonNode getDefaultValue() {
        return this.defaultValue;
    }

    public void setDefaultValue(@Nullable JsonNode defaultValue) {
        this.defaultValue = defaultValue;
        this.touchDateModified();
    }

    public boolean isSystemProperty() {
        return this.systemProperty;
    }

    public void setSystemProperty(boolean systemProperty) {
        this.systemProperty = systemProperty;
        this.touchDateModified();
    }

    @Override
    public ComponentType<?> getComponentType() {
        return ComponentTypes.OBJECT_PROPERTY;
    }
}
