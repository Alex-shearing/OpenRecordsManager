package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;
import com.openrecordsmanager.resources.ResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "record_type")
public class RecordType {
    @Id
    @JsonProperty
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

    @Column(nullable = false)
    @JsonProperty
    @JdbcTypeCode(SqlTypes.JSON)
    public Set<ResourceIdentifier> properties;

    @Deprecated
    protected RecordType() {
    }

    public RecordType(ResourceIdentifier id, String name, String description, @Nullable Set<String> contentTypes, @Nullable String securityFilter, SecurityFilterUsage securityFilterUsage) {
        this.id = id;
        this.description = description;
        this.name = name;
        this.contentTypes = contentTypes;
        this.securityFilter = securityFilter;
        this.securityFilterUsage = securityFilterUsage;
        this.properties = new HashSet<>();
    }

    public RecordType(ResourceIdentifier id, ResourceRegistry registry, RecordTypeDefinition definition) {
        this(id, definition.name(), definition.description(), definition.allowedContentTypes(), definition.securityFilter(), definition.securityFilterUsage());
        this.properties = definition.properties()
                .stream()
                .map(def -> registry.getResourceId(ResourceType.PROPERTY, def))
                .collect(Collectors.toSet());
    }

    public boolean supportsFile() {
        return this.contentTypes != null && !this.contentTypes.isEmpty();
    }
}
