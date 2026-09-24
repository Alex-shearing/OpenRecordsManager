package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "list_element")
@SuppressWarnings("NotNullFieldNotInitialized")
public class ListElement implements IListElement {
    @Id
    @JavaType(ResourceIdentifierJavaType.class)
    private ResourceIdentifier id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private ListType parent;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int elementIndex;

    @Column
    @Nullable
    private Instant activeTo;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column(nullable = false)
    private Instant dateModified;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "list_element_alias",
            joinColumns = @JoinColumn(name = "list_element_id")
    )
    private Set<String> aliases = new HashSet<>();

    @Deprecated
    protected ListElement() {
    }

    public ListElement(
            ResourceIdentifier id,
            ListType parent,
            String name,
            String description,
            int elementIndex,
            @Nullable Instant activeTo,
            Set<String> aliases
    ) {
        this.id = id;
        this.parent = parent;
        this.name = name;
        this.description = description;
        this.elementIndex = elementIndex;
        this.activeTo = activeTo;
        this.aliases = aliases;
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    public ResourceIdentifier getId() {
        return this.id;
    }

    public ListType getParent() {
        return this.parent;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.touchDateModified();
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.touchDateModified();
    }

    public int getElementIndex() {
        return this.elementIndex;
    }

    public void setElementIndex(int elementIndex) {
        this.elementIndex = elementIndex;
        this.touchDateModified();
    }

    public @Nullable Instant getActiveTo() {
        return this.activeTo;
    }

    public void setActiveTo(@Nullable Instant activeTo) {
        this.activeTo = activeTo;
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

    public Set<String> getAliases() {
        return this.aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
        this.touchDateModified();
    }

    @Override
    public int index() {
        return this.elementIndex;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListElement that)) {
            return false;
        }
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
