package com.openrecordsmanager.recordtype;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@Entity
@Table(name = "record_type")
public class RecordType {
    @Id
    @JsonProperty
    @JavaType(ResourceIdentifierJavaType.class)
    public ResourceIdentifier id;

    @Column(nullable = false)
    @JsonProperty
    public String name;

    @Column(nullable = false)
    @JsonProperty
    public String description;

    @Column()
    @JsonProperty
    @Nullable
    public String securityFilter;

    @Column(nullable = false)
    @JsonProperty
    public SecurityFilterUsage securityFilterUsage;

    @Column()
    @JsonProperty
    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    public Set<String> contentTypes;

    @JsonProperty
    @ElementCollection
    @CollectionTable(
            name = "record_type_property",
            joinColumns = @JoinColumn(name = "record_type")
    )
    public Set<RecordTypeProperty<?>> properties;

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
        this.contentTypes = contentTypes;
        this.securityFilter = securityFilter;
        this.securityFilterUsage = securityFilterUsage;
        this.properties = properties;
    }

    public boolean supportsFile() {
        return this.contentTypes != null && !this.contentTypes.isEmpty();
    }
}
