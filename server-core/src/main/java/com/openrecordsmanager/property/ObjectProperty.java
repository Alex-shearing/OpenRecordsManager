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
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "object_property")
@SuppressWarnings("NotNullFieldNotInitialized")
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
    private JsonNode defaultValue;

    @Column(nullable = false)
    private boolean userHidden;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column(nullable = false)
    private Instant dateModified;

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
            @Nullable JsonNode defaultValue,
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
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    public ObjectProperty(ResourceIdentifier identifier, String name, String description, PropertyType<T> type) {
        this(identifier, name, description, type, null, null, null, null, false);
    }

    public ResourceIdentifier getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.touchDateModified();
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.touchDateModified();
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

    public boolean isUserHidden() {
        return this.userHidden;
    }

    public void setUserHidden(boolean userHidden) {
        this.userHidden = userHidden;
        this.touchDateModified();
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ObjectProperty<?> that = (ObjectProperty<?>) o;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

}
