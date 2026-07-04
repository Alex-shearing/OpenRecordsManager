package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.IListElement;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "list_element")
@JsonSerialize(using = ListElement.Serializer.class)
public class ListElement implements IListElement {
    @Id
    @JsonProperty
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

    public static class Serializer extends ValueSerializer<ListElement> {

        @Override
        public void serialize(ListElement value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeStartObject();
            gen.writeStringProperty("id", value.id.toString());
            gen.writeStringProperty("name", value.name);
            gen.writeStringProperty("description", value.description);
            gen.writeNumberProperty("index", value.index());
            if (value.activeTo != null) gen.writePOJOProperty("activeTo", value.activeTo);

            gen.writeArrayPropertyStart("aliases");
            value.aliases.forEach(gen::writeString);
            gen.writeEndArray();

            gen.writeEndObject();
        }
    }

}
