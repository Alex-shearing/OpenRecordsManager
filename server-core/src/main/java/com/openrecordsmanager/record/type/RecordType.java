package com.openrecordsmanager.record.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.template.ManagedObjectType;
import com.openrecordsmanager.template.ManagedObjectTypeProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "record_type")
@DiscriminatorValue("record_type")
@PrimaryKeyJoinColumn(name = "id")
public class RecordType extends ManagedObjectType {
    @Column()
    @Nullable
    private String securityFilter;

    @Column(nullable = false)
    private SecurityFilterUsage securityFilterUsage;

    @Column()
    @JdbcTypeCode(SqlTypes.JSON)
    private Set<String> contentTypes = new HashSet<>();

    /**
     * Subselect (not join) so an EAGER load does not multiply rows against {@code baseType}'s
     * properties and inflate this set under flush.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(
            name = "record_type_property",
            joinColumns = @JoinColumn(name = "record_type")
    )
    private Set<ManagedObjectTypeProperty<?>> properties = new HashSet<>();

    @Deprecated
    protected RecordType() {
    }

    public RecordType(
            ResourceIdentifier id,
            @Nullable Set<String> contentTypes,
            @Nullable String securityFilter,
            SecurityFilterUsage securityFilterUsage,
            Set<ManagedObjectTypeProperty<?>> properties
    ) {
        this(id, false, null, contentTypes, securityFilter, securityFilterUsage, properties);
    }

    public RecordType(
            ResourceIdentifier id,
            boolean abstractType,
            @Nullable ManagedObjectType baseType,
            @Nullable Set<String> contentTypes,
            @Nullable String securityFilter,
            SecurityFilterUsage securityFilterUsage,
            Set<ManagedObjectTypeProperty<?>> properties
    ) {
        super(id, abstractType, baseType);
        this.contentTypes = contentTypes != null ? contentTypes : new HashSet<>();
        this.securityFilter = securityFilter;
        this.securityFilterUsage = securityFilterUsage;
        this.properties = properties;
    }

    public @Nullable String getSecurityFilter() {
        return this.securityFilter;
    }

    public SecurityFilterUsage getSecurityFilterUsage() {
        return this.securityFilterUsage;
    }

    @Override
    public Set<ManagedObjectTypeProperty<?>> getDeclaredProperties() {
        return this.properties;
    }

    public Set<String> getContentTypes() {
        return this.contentTypes;
    }

    public void setSecurityFilter(@Nullable String securityFilter) {
        this.securityFilter = securityFilter;
        this.touchDateModified();
    }

    public void setSecurityFilterUsage(SecurityFilterUsage securityFilterUsage) {
        this.securityFilterUsage = securityFilterUsage;
        this.touchDateModified();
    }

    public void setContentTypes(@Nullable Set<String> contentTypes) {
        this.contentTypes = contentTypes != null ? new HashSet<>(contentTypes) : new HashSet<>();
        this.touchDateModified();
    }

    public void setProperties(Set<ManagedObjectTypeProperty<?>> properties) {
        this.properties = properties;
        this.touchDateModified();
    }

    public boolean supportsFile() {
        return !this.contentTypes.isEmpty();
    }

    @Override
    public ManagedObjectKind getKind() {
        return ManagedObjectKind.RECORD;
    }

    public SecurityFilterUsage securityFilter(ExpressionsService expressions, Record record, User actor) {
        RecordType cursor = this;
        while (cursor != null) {
            if (cursor.securityFilter != null && !expressions.checkPropertyExpression(
                    record.getId(),
                    cursor.securityFilter,
                    null,
                    actor,
                    record
            )) {
                return cursor.securityFilterUsage;
            }
            ManagedObjectType base = cursor.getBaseType();
            cursor = base instanceof RecordType recordBase ? recordBase : null;
        }

        for (ManagedObjectTypeProperty<?> binding : this.getEffectiveProperties()) {
            String propertyFilter = binding.getProperty().getSecurityFilter();
            if (propertyFilter != null && !expressions.checkPropertyExpression(
                    record.getId(),
                    propertyFilter,
                    record.getProperty(binding.getProperty()),
                    actor,
                    record
            )) {
                return this.securityFilterUsage;
            }
        }

        return SecurityFilterUsage.SHOW_ALL;
    }

    @Override
    public ComponentType<?> getComponentType() {
        return ComponentTypes.RECORD_TYPE;
    }
}
