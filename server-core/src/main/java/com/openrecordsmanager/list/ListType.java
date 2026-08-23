package com.openrecordsmanager.list;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "list_type")
public class ListType {
    @Id
    @JsonProperty
    @JavaType(ResourceIdentifierJavaType.class)
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
