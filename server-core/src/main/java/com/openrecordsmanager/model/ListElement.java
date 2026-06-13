package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.openrecordsmanager.list.ListItem;
import com.openrecordsmanager.model.util.DbResourceIdentifier;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "list_element")
@JsonPropertyOrder({"id", "display", "parent", "description", "activeTo", "aliases"})
public class ListElement {
    @EmbeddedId
    @JsonProperty
    public DbResourceIdentifier id;

    @ManyToOne
    @JoinColumn(name = "parent_id")
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

    public ListElement() {
    }

    public ListElement fromDefinition(ListType type, ListItem def) {
        this.parent = type;
        this.display = def.display();
        this.description = def.description();
        this.elementIndex = def.index();
        this.activeTo = def.activeTo();
        this.aliases = def.aliases();
        return this;
    }
}
