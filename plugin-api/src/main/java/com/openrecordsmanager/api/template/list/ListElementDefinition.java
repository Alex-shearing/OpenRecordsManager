package com.openrecordsmanager.api.template.list;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

@NullMarked
public record ListElementDefinition(
        String name,
        @JsonSetter(nulls = Nulls.AS_EMPTY) String description,
        int index,
        @Nullable Date activeTo,
        @JsonSetter(nulls = Nulls.AS_EMPTY) Set<String> aliases,
        ComponentReference<ListDefinition> parent
) implements Component {

    public ListElementDefinition {
        Objects.requireNonNull(name, "Property 'name' must not be null");
        Objects.requireNonNull(description, "Property 'description' must not be null");
        Objects.requireNonNull(aliases, "Property 'aliases' must not be null");
        Objects.requireNonNull(parent, "Property 'parent' must not be null");
    }

    @Override
    public Set<ComponentReference<? extends Component>> getDependencies() {
        return Set.of(this.parent);
    }

    public static class Builder {
        private final ListDefinition.Builder parentBuilder;
        private final String id;
        private final String name;
        private final Set<String> aliases = new HashSet<>();

        private String description = "";
        private int index = 0;
        @Nullable
        private Date activeTo = null;

        public Builder(ListDefinition.Builder parentBuilder, String id, String name) {
            this.parentBuilder = parentBuilder;
            this.id = id;
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the index of the list item. Index is NOT unique and should not be treated as a key.
         * Index is primarily used to indicate sort order, with lower values displayed before
         * higher values.
         */
        public Builder index(int index) {
            this.index = index;
            return this;
        }

        public Builder activeTo(Calendar activeTo) {
            this.activeTo = activeTo.getTime();
            return this;
        }

        public Builder alias(String alias) {
            this.aliases.add(alias);
            return this;
        }

        public ListDefinition.Builder endEntry() {
            this.parentBuilder.addEntry(this.id, this);
            return this.parentBuilder;
        }

        public ListElementDefinition build(ComponentReference<ListDefinition> parent) {
            return new ListElementDefinition(
                    this.name,
                    this.description,
                    this.index,
                    this.activeTo,
                    this.aliases,
                    parent
            );
        }
    }
}
