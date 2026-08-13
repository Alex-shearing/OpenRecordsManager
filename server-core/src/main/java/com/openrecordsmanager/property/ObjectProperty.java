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
    public ResourceIdentifier id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String description;

    @Column(nullable = false)
    @Convert(converter = PropertyTypeConverter.class)
    @JavaType(ObjectJavaType.class)
    public PropertyType<T> type;

    @ManyToOne
    @JoinColumn
    @Nullable
    public ListType listType;

    @Column()
    @Nullable
    public String validator;

    @Column()
    @Nullable
    public String securityFilter;

    @Column()
    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    public T defaultValue;

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
            @Nullable T defaultValue
    ) {
        this.id = identifier;
        this.name = name;
        this.description = description;
        this.type = type;
        this.listType = listType;
        this.validator = validator;
        this.securityFilter = securityFilter;
        this.defaultValue = defaultValue;
    }

    public ObjectProperty(ResourceIdentifier identifier, String name, String description, PropertyType<T> type) {
        this(identifier, name, description, type, null, null, null, null);
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
