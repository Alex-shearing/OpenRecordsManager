package com.openrecordsmanager.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.database.util.PropertyTypeConverter;
import com.openrecordsmanager.list.ListType;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

@Entity
@Table(name = "object_property")
@JsonSerialize(using = ObjectProperty.Serializer.class)
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
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ObjectProperty<?> that = (ObjectProperty<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public static class Serializer extends ValueSerializer<ObjectProperty<?>> {
        @Override
        public void serialize(ObjectProperty<?> value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();

            gen.writeStringProperty("id", value.id.toString());
            gen.writeStringProperty("name", value.name);
            gen.writeStringProperty("type", value.type.name);
            if (value.listType != null) {
                gen.writeStringProperty("list_type", value.listType.id.toString());
            }

            gen.writeEndObject();
        }
    }
}
