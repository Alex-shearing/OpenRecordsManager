package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.api.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.api.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ResourceTypes;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
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

    public RecordType(ResourceIdentifier id, String name, String description, @Nullable Set<String> contentTypes, @Nullable String securityFilter, SecurityFilterUsage securityFilterUsage) {
        this.id = id;
        this.description = description;
        this.name = name;
        this.contentTypes = contentTypes;
        this.securityFilter = securityFilter;
        this.securityFilterUsage = securityFilterUsage;
        this.properties = new HashSet<>();
    }

    public RecordType(ResourceIdentifier id, ResourceCatalog catalog, ExpressionsService expressions, DataRepository repository, RecordTypeDefinition definition) {
        this(id, definition.name(), definition.description(), definition.allowedContentTypes(), expressions.buildExpression(definition.securityFilter()), definition.securityFilterUsage());
        this.properties = definition.properties().entrySet()
                .stream()
                .map(def -> createRecordTypeProperty(def, catalog, repository))
                .collect(Collectors.<RecordTypeProperty<?>>toSet());
    }

    @SuppressWarnings("unchecked")
    private <T> RecordTypeProperty<T> createRecordTypeProperty(Map.Entry<PropertyDefinition<?>, ?> entry, ResourceCatalog catalog, DataRepository repository) {
        ResourceIdentifier propId = catalog.getResourceId(ResourceTypes.PROPERTY, entry.getKey());
        if (propId == null) {
            throw new IllegalArgumentException("Attempted to use property that was not in catalog: " + entry.getKey().getName());
        }

        Optional<ObjectProperty<?>> property = ResourceTypes.PROPERTY.getRegistered(propId, repository);
        if (property.isEmpty()) {
            throw new IllegalArgumentException("Attempted to use property that was not registered: " + propId);
        }

        return new RecordTypeProperty<>((ObjectProperty<T>) property.get(), (T) entry.getValue());
    }

    public boolean supportsFile() {
        return this.contentTypes != null && !this.contentTypes.isEmpty();
    }
}
