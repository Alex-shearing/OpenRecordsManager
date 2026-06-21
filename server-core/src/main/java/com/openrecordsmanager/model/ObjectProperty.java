package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.model.repositories.ListTypeRepository;
import com.openrecordsmanager.property.PropertyDefinition;
import com.openrecordsmanager.property.PropertyType;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;
import com.openrecordsmanager.resources.ResourceType;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

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

    @Column(name = "type", nullable = false)
    private String typeDbValue;

    @JsonProperty
    @Transient
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

    @Deprecated
    protected ObjectProperty() {
    }

    public ObjectProperty(ResourceIdentifier identifier, String name, String description, PropertyType<T> type) {
        this.id = identifier;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public ObjectProperty(ResourceIdentifier identifier, ListTypeRepository repository, ResourceRegistry registry, PropertyDefinition<T> definition) {
        this(identifier, definition.getName(), definition.getDescription(), definition.getType());
        if (definition.getListType() != null) {
            this.listType = repository.findById(registry.getResourceId(ResourceType.LIST, definition.getListType())).orElseThrow();
        }
        this.validator = definition.getValidator();
        this.securityFilter = definition.getSecurityFilter();
    }

    @PostLoad
    @SuppressWarnings("unchecked")
    private void onLoad() {
        if (this.typeDbValue != null) {
            this.type = (PropertyType<T>) PropertyType.TYPES.get(this.typeDbValue);
        }
    }

    @PrePersist
    @PreUpdate
    private void onSave() {
        if (this.type != null) {
            this.typeDbValue = this.type.name;
        }
    }
}
