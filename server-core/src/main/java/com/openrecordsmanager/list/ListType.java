package com.openrecordsmanager.list;

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
    @JavaType(ResourceIdentifierJavaType.class)
    private ResourceIdentifier id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "parent")
    @OrderBy("elementIndex ASC")
    private List<ListElement> children = new ArrayList<>();

    @Deprecated
    protected ListType() {
    }

    public ListType(ResourceIdentifier id, String name) {
        this.id = id;
        this.name = name;
    }

    public ResourceIdentifier getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ListElement> getChildren() {
        return this.children;
    }
}
