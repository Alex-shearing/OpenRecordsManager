package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import com.openrecordsmanager.template.RegisteredComponent;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "list_type")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class ListType implements RegisteredComponent {
    @Id
    @JavaType(ResourceIdentifierJavaType.class)
    private ResourceIdentifier id;

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

    public ListType(ResourceIdentifier id) {
        this.id = id;
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    public ResourceIdentifier getId() {
        return this.id;
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

    @Override
    public ComponentType<?> getComponentType() {
        return ComponentTypes.LIST;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ListType listType)) return false;
        return Objects.equals(this.id, listType.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
