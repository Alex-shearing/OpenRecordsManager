package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.template.RegisteredComponent;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "list_element")
@DiscriminatorValue("list_element")
@PrimaryKeyJoinColumn(name = "id")
@SuppressWarnings("NotNullFieldNotInitialized")
public class ListElement extends RegisteredComponent implements IListElement {
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private ListType parent;

    @Column(nullable = false)
    private int elementIndex;

    @Column
    @Nullable
    private Instant activeTo;

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
            int elementIndex,
            @Nullable Instant activeTo,
            Set<String> aliases
    ) {
        super(id);
        this.parent = parent;
        this.elementIndex = elementIndex;
        this.activeTo = activeTo;
        this.aliases = aliases;
    }

    public ListType getParent() {
        return this.parent;
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
    public ComponentType<?> getComponentType() {
        return ComponentTypes.LIST_ELEMENT;
    }
}
