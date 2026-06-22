package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.resources.ResourceIdentifier;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "list_type")
@JsonPropertyOrder({"id", "display"})
public class ListType {
    @Id
    @JsonProperty
    public ResourceIdentifier id;

    @Column(nullable = false)
    @JsonProperty
    public String display;

    @OneToMany(mappedBy = "parent")
    @OrderBy("elementIndex ASC")
    @JsonProperty
    public List<ListElement> children;

    @Deprecated
    protected ListType() {
    }

    public ListType(ResourceIdentifier id, ListDefinition def) {
        this(id, def.display);
    }

    public ListType(ResourceIdentifier id, String display) {
        this.id = id;
        this.display = display;
        this.children = new ArrayList<>();
    }
}
