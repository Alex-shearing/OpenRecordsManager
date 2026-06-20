package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.openrecordsmanager.list.IListElement;
import com.openrecordsmanager.list.ListItemDef;
import com.openrecordsmanager.model.util.DbResourceIdentifier;
import com.openrecordsmanager.resources.ResourceIdentifier;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelFunctionBinding;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "list_element")
@JsonPropertyOrder({"id", "display", "parent", "description", "activeTo", "aliases"})
public class ListElement implements IListElement {
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

    public ListElement(ResourceIdentifier id, ListType parent, String display, String description, int elementIndex, Date activeTo, Set<String> aliases) {
        this.id = new DbResourceIdentifier(id);
        this.parent = parent;
        this.display = display;
        this.description = description;
        this.elementIndex = elementIndex;
        this.activeTo = activeTo;
        this.aliases = aliases;
    }

    public ListElement fromDefinition(ListType type, ListItemDef def) {
        this.parent = type;
        this.display = def.display();
        this.description = def.description();
        this.elementIndex = def.index();
        this.activeTo = def.activeTo();
        this.aliases = def.aliases();
        return this;
    }

    @Override
    public int index() {
        return this.elementIndex;
    }
    
    public static List<CelFunctionDecl> getCompilerDeclaration() {
        return List.of(
                CelFunctionDecl.newFunctionDeclaration(
                        "_>=_",
                        CelOverloadDecl.newGlobalOverload(
                                "greater_equals_list_element",
                                SimpleType.BOOL,
                                SimpleType.DYN,
                                SimpleType.DYN
                        )
                ),
                CelFunctionDecl.newFunctionDeclaration(
                        "_>_",
                        CelOverloadDecl.newGlobalOverload(
                                "greater_list_element",
                                SimpleType.BOOL,
                                SimpleType.DYN,
                                SimpleType.DYN
                        )
                ),

                CelFunctionDecl.newFunctionDeclaration(
                        "_<=_",
                        CelOverloadDecl.newGlobalOverload(
                                "less_equals_list_element",
                                SimpleType.BOOL,
                                SimpleType.DYN,
                                SimpleType.DYN
                        )
                ),
                CelFunctionDecl.newFunctionDeclaration(
                        "_<_",
                        CelOverloadDecl.newGlobalOverload(
                                "less_list_element",
                                SimpleType.BOOL,
                                SimpleType.DYN,
                                SimpleType.DYN
                        )
                )
        );
    }

    public static List<CelFunctionBinding> getRuntimeBinding() {
        return List.of(
                CelFunctionBinding.from(
                        "greater_equals_list_element",
                        ListElement.class,
                        ListElement.class,
                        (first, second) -> first.index() >= second.index()
                ),
                CelFunctionBinding.from(
                        "greater_list_element",
                        ListElement.class,
                        ListElement.class,
                        (first, second) -> first.index() > second.index()
                ),

                CelFunctionBinding.from(
                        "less_equals_list_element",
                        ListElement.class,
                        ListElement.class,
                        (first, second) -> first.index() <= second.index()
                ),
                CelFunctionBinding.from(
                        "less_list_element",
                        ListElement.class,
                        ListElement.class,
                        (first, second) -> first.index() < second.index()
                )
        );
    }
}
