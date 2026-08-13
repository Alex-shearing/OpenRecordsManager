package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.database.util.PropertyTypeConverter;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import com.openrecordsmanager.list.ListType;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@Table(name = "object_property")
public class ObjectProperty<T> {
    @Id
    @JavaType(ResourceIdentifierJavaType.class)
    private ResourceIdentifier id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

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
    private T defaultValue;

    @Column(nullable = false)
    private boolean userHidden;

    @Deprecated
    protected ObjectProperty() {
    }

    public ObjectProperty(
            ResourceIdentifier identifier,
            String name,
            String description,
            PropertyType<T> type,
            @Nullable ListType listType,
            @Nullable String validator,
            @Nullable String securityFilter,
            @Nullable T defaultValue,
            boolean userHidden
    ) {
        this.id = identifier;
        this.name = name;
        this.description = description;
        this.type = type;
        this.listType = listType;
        this.validator = validator;
        this.securityFilter = securityFilter;
        this.defaultValue = defaultValue;
        this.userHidden = userHidden;
    }

    public ObjectProperty(ResourceIdentifier identifier, String name, String description, PropertyType<T> type) {
        this(identifier, name, description, type, null, null, null, null, false);
    }

    public ResourceIdentifier getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PropertyType<T> getType() {
        return type;
    }

    public void setType(PropertyType<T> type) {
        this.type = type;
    }

    public @Nullable ListType getListType() {
        return listType;
    }

    public void setListType(@Nullable ListType listType) {
        this.listType = listType;
    }

    public @Nullable String getValidator() {
        return validator;
    }

    public void setValidator(@Nullable String validator) {
        this.validator = validator;
    }

    public @Nullable String getSecurityFilter() {
        return securityFilter;
    }

    public void setSecurityFilter(@Nullable String securityFilter) {
        this.securityFilter = securityFilter;
    }

    public @Nullable T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(@Nullable T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isUserHidden() {
        return userHidden;
    }

    public void setUserHidden(boolean userHidden) {
        this.userHidden = userHidden;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ObjectProperty<?> that = (ObjectProperty<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
