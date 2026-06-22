package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.openrecordsmanager.api.list.IListElement;
import com.openrecordsmanager.api.list.ListElementDefinition;
import com.openrecordsmanager.resources.ResourceIdentifier;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "list_element")
@JsonPropertyOrder({"id", "display", "parent", "description", "activeTo", "aliases"})
public class ListElement implements IListElement {
    @Id
    @JsonProperty
    public ResourceIdentifier id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    public ListType parent;

    @Column(nullable = false)
    @JsonProperty
    public String display;

    @Column(nullable = false)
    @JsonProperty
    public String description;

    @Column(nullable = false)
    @JsonProperty
    public int elementIndex;

    @Column()
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Date activeTo;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Set<String> aliases;

    @Deprecated
    protected ListElement() {
    }

    public ListElement(ResourceIdentifier id, ListType parent, ListElementDefinition def) {
        this(id, parent, def.display(), def.description(), def.index(), def.activeTo(), def.aliases());
    }

    public ListElement(ResourceIdentifier id, ListType parent, String display, String description, int elementIndex, Date activeTo, Set<String> aliases) {
        this.id = id;
        this.parent = parent;
        this.display = display;
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
