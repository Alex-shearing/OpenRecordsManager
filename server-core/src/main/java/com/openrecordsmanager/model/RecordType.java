package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.api.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;
import com.openrecordsmanager.resources.types.ResourceTypes;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
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
    @ManyToMany(fetch = FetchType.EAGER)
    public Set<ObjectProperty<?>> properties;

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

    public RecordType(ResourceIdentifier id, ResourceRegistry registry, ExpressionsService expressions, DataRepository repository, RecordTypeDefinition definition) {
        this(id, definition.name(), definition.description(), definition.allowedContentTypes(), expressions.buildExpression(definition.securityFilter()), definition.securityFilterUsage());
        this.properties = definition.properties()
                .stream()
                .map(def -> {
                    ResourceIdentifier propId = registry.getResourceId(ResourceTypes.PROPERTY, def);
                    if (propId == null) {
                        throw new IllegalArgumentException("Attempted to use property that was not registered: " + def.getName());
                    }
                    Optional<ObjectProperty<?>> prop = repository.objectPropertyRepo.findById(propId);
                    if (prop.isEmpty()) {
                        throw new IllegalArgumentException("Attempted to use property that does not exist: " + propId);
                    }

                    return prop.get();
                })
                .collect(Collectors.toSet());
    }

    public boolean supportsFile() {
        return this.contentTypes != null && !this.contentTypes.isEmpty();
    }
}
