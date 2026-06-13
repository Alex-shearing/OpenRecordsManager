package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.model.repositories.ListTypeRepository;
import com.openrecordsmanager.model.util.DbResourceIdentifier;
import com.openrecordsmanager.property.PropertyDefinition;
import com.openrecordsmanager.property.PropertyType;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;
import jakarta.persistence.*;

@Entity
@Table(name = "record_property")
public class RecordProperty {
    @EmbeddedId
    @JsonProperty
    public DbResourceIdentifier id;

    @Column(nullable = false)
    @JsonProperty
    public String name;

    @Column(nullable = false)
    @JsonProperty
    public String description;

    @Column(nullable = false)
    @JsonProperty
    public PropertyType<?> type;

    @ManyToOne
    @JoinColumn(name = "list_type_id")
    @JsonProperty
    public ListType listType;

    protected RecordProperty() {
    }

    public RecordProperty(ResourceIdentifier identifier) {
        this.id = new DbResourceIdentifier(identifier);
    }

    public RecordProperty fromDefinition(ListTypeRepository repository, ResourceRegistry registry, PropertyDefinition<?> definition) {
        this.name = definition.getName();
        this.description = definition.getDescription();
        this.type = definition.getType();
        if (definition.getListType() != null) {
            this.listType = repository.findById(registry.getResourceId(definition.getListType())).orElseThrow();
        }

        return this;
    }
}
