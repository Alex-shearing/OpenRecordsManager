package com.openrecordsmanager.recordtype;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "record_type")
@SuppressWarnings("NotNullFieldNotInitialized")
public class RecordType {
    @Id
    @JavaType(ResourceIdentifierJavaType.class)
    private ResourceIdentifier id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column()
    @Nullable
    private String securityFilter;

    @Column(nullable = false)
    private SecurityFilterUsage securityFilterUsage;

    @Column()
    @JdbcTypeCode(SqlTypes.JSON)
    private Set<String> contentTypes = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "record_type_property",
            joinColumns = @JoinColumn(name = "record_type")
    )
    private Set<RecordTypeProperty<?>> properties = new HashSet<>();

    @Deprecated
    protected RecordType() {
    }

    public RecordType(
            ResourceIdentifier id,
            String name,
            String description,
            @Nullable Set<String> contentTypes,
            @Nullable String securityFilter,
            SecurityFilterUsage securityFilterUsage,
            Set<RecordTypeProperty<?>> properties
    ) {
        this.id = id;
        this.description = description;
        this.name = name;
        this.contentTypes = contentTypes != null ? contentTypes : new HashSet<>();
        this.securityFilter = securityFilter;
        this.securityFilterUsage = securityFilterUsage;
        this.properties = properties;
    }

    public ResourceIdentifier getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public @Nullable String getSecurityFilter() {
        return this.securityFilter;
    }

    public SecurityFilterUsage getSecurityFilterUsage() {
        return this.securityFilterUsage;
    }

    public Set<RecordTypeProperty<?>> getProperties() {
        return this.properties;
    }

    public Set<String> getContentTypes() {
        return this.contentTypes;
    }

    public boolean supportsFile() {
        return !this.contentTypes.isEmpty();
    }

    public boolean hasProperty(ObjectProperty<?> property) {
        return this.properties.stream()
                .anyMatch(prop -> Objects.equals(prop.getProperty(), property));
    }

    public SecurityFilterUsage securityFilter(ExpressionsService expressions, Record record, User actor) {
        if (this.securityFilter != null && !expressions.checkPropertyExpression(
                record.getId(),
                this.securityFilter,
                null,
                actor,
                record
        )) {
            return this.securityFilterUsage;
        }

        for (RecordTypeProperty<?> recordTypeProperty : this.properties) {
            if (!recordTypeProperty.securityFilter(expressions, record, actor)) {
                return this.securityFilterUsage;
            }
        }

        return SecurityFilterUsage.SHOW_ALL;
    }
}
