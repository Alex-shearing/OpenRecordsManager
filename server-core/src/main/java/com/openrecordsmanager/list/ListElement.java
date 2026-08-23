package com.openrecordsmanager.list;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "list_element")
public class ListElement implements IListElement {
    @Id
    @JsonProperty
    @JavaType(ResourceIdentifierJavaType.class)
    public ResourceIdentifier id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    public ListType parent;

    @Column(nullable = false)
    @JsonProperty
    public String name;

    @Column(nullable = false)
    @JsonProperty
    public String description;

    @Column(nullable = false)
    @JsonProperty
    public int elementIndex;

    @Column()
    @JsonProperty
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Date activeTo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "list_element_alias",
            joinColumns = @JoinColumn(name = "list_element_id")
    )
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Set<String> aliases;

    @Deprecated
    protected ListElement() {
    }

    public ListElement(
            ResourceIdentifier id,
            ListType parent,
            String name,
            String description,
            int elementIndex,
            @Nullable Date activeTo,
            Set<String> aliases
    ) {
        this.id = id;
        this.parent = parent;
        this.name = name;
        this.description = description;
        this.elementIndex = elementIndex;
        this.activeTo = activeTo;
        this.aliases = aliases;
    }

    @Override
    public int index() {
        return this.elementIndex;
    }

}
