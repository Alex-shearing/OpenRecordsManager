package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "list_type")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class ListType {
    @Id
    @JavaType(ResourceIdentifierJavaType.class)
    private ResourceIdentifier id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column(nullable = false)
    private Instant dateModified;

    @OneToMany(mappedBy = "parent")
    @OrderBy("elementIndex ASC")
    private List<ListElement> children = new ArrayList<>();

    @Deprecated
    protected ListType() {
    }

    public ListType(ResourceIdentifier id, String name) {
        this.id = id;
        this.name = name;
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    public ResourceIdentifier getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.touchDateModified();
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
    }

    public List<ListElement> getChildren() {
        return this.children;
    }
}
