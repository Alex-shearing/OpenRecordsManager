package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.openrecordsmanager.list.ListDefinition;
import com.openrecordsmanager.model.util.DbResourceIdentifier;
import com.openrecordsmanager.resources.ResourceIdentifier;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "list_type")
@JsonPropertyOrder({"id", "display"})
public class ListType {
    @EmbeddedId
    @JsonProperty
    public DbResourceIdentifier id;

    @Column(nullable = false)
    @JsonProperty
    public String display;

    @OneToMany(mappedBy = "parent")
    @OrderBy("elementIndex ASC")
    @JsonProperty
    public List<ListElement> children;

    public ListType() {
        this.children = new ArrayList<>();
    }

    public ListType(ResourceIdentifier id) {
        this.id = new DbResourceIdentifier(id);
        this();
    }

    public ListType fromDefinition(ListDefinition def) {
        this.display = def.display;
        return this;
    }
}
