package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.model.repositories.ListTypeRepository;
import com.openrecordsmanager.model.util.DbResourceIdentifier;
import com.openrecordsmanager.property.PropertyDefinition;
import com.openrecordsmanager.property.PropertyType;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;
import com.openrecordsmanager.resources.ResourceType;
import jakarta.persistence.*;

@Entity
@Table(name = "object_property")
public class ObjectProperty<T> {
    @EmbeddedId
    @JsonProperty
    public DbResourceIdentifier id;

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
    @JoinColumn(name = "list_type_id")
    @JsonProperty
    public ListType listType;

    @Column(nullable = false)
    @JsonProperty
    public String securityFilter;

    protected ObjectProperty() {
    }

    public ObjectProperty(ResourceIdentifier identifier) {
        this.id = new DbResourceIdentifier(identifier);
    }

    public ObjectProperty<T> fromDefinition(ListTypeRepository repository, ResourceRegistry registry, PropertyDefinition<T> definition) {
        this.name = definition.getName();
        this.description = definition.getDescription();
        this.type = definition.getType();
        if (definition.getListType() != null) {
            this.listType = repository.findById(registry.getResourceId(ResourceType.LIST, definition.getListType())).orElseThrow();
        }
        this.securityFilter = definition.getSecurityFilter();

        return this;
    }

    @PostLoad
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
