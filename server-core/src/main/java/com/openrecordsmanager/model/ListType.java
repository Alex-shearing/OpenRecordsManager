package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "list_type")
@JsonPropertyOrder({"id", "name"})
public class ListType {
    @Id
    @JsonProperty
    public ResourceIdentifier id;

    @Column(nullable = false)
    @JsonProperty
    public String name;

    @OneToMany(mappedBy = "parent")
    @OrderBy("elementIndex ASC")
    @JsonProperty
    public List<ListElement> children;

    @Deprecated
    protected ListType() {
    }

    public ListType(ResourceIdentifier id, String name) {
        this.id = id;
        this.name = name;
        this.children = new ArrayList<>();
    }
}
