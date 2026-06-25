package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.api.property.PropertyType;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.model.util.PropertyTypeConverter;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ResourceTypes;
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
    @JsonProperty
    public ResourceIdentifier id;

    @Column(nullable = false)
    @JsonProperty
    public String name;

    @Column(nullable = false)
    @JsonProperty
    public String description;

    @Column(nullable = false)
    @Convert(converter = PropertyTypeConverter.class)
    @JavaType(ObjectJavaType.class)
    @JsonProperty
    public PropertyType<T> type;

    @ManyToOne
    @JoinColumn
    @JsonProperty
    @Nullable
    public ListType listType;

    @Column()
    @JsonProperty
    @Nullable
    public String validator;

    @Column()
    @JsonProperty
    @Nullable
    public String securityFilter;

    @Column()
    @JsonProperty
    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    public T defaultValue;

    @Deprecated
    protected ObjectProperty() {
    }

    public ObjectProperty(ResourceIdentifier identifier, String name, String description, PropertyType<T> type) {
        this.id = identifier;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public ObjectProperty(ResourceIdentifier identifier, ResourceCatalog registry, ExpressionsService expressions, DataRepository repository, PropertyDefinition<T> definition) {
        this(identifier, definition.getName(), definition.getDescription(), definition.getType());
        if (definition.getType().allowsList() && definition.getListType() != null) {
            ResourceIdentifier listId = registry.getResourceId(ResourceTypes.LIST, definition.getListType());
            if (listId == null) {
                throw new IllegalArgumentException("ListType " + definition.getListType().id() + " does not exist");
            }
            this.listType = repository.listTypeRepo.findById(listId).orElseThrow();
        }
        this.validator = expressions.buildExpression(definition.getValidator());
        this.securityFilter = expressions.buildExpression(definition.getSecurityFilter());
        this.defaultValue = definition.getDefaultValue();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ObjectProperty<?> that = (ObjectProperty<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
