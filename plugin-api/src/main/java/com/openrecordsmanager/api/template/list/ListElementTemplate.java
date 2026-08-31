package com.openrecordsmanager.api.template.list;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.TemplateComponent;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public record ListElementTemplate(
        String name,
        @JsonSetter(nulls = Nulls.AS_EMPTY) String description,
        int index,
        @Nullable Instant activeTo,
        @JsonSetter(nulls = Nulls.AS_EMPTY) Set<String> aliases,
        ComponentReference<ListTemplate> parent
) implements TemplateComponent {

    public ListElementTemplate {
        Objects.requireNonNull(name, "Property 'name' must not be null");
        Objects.requireNonNull(description, "Property 'description' must not be null");
        Objects.requireNonNull(aliases, "Property 'aliases' must not be null");
        Objects.requireNonNull(parent, "Property 'parent' must not be null");
    }

    /**
     * The reference to the parent list is not considered for the equals operations.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ListElementTemplate that)) return false;
        return index == that.index &&
                Objects.equals(name, that.name) &&
                Objects.equals(activeTo, that.activeTo) &&
                Objects.equals(description, that.description) &&
                Objects.equals(aliases, that.aliases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, index, activeTo, aliases);
    }

    @Override
    public Set<ComponentReference<? extends TemplateComponent>> getDependencies() {
        return Set.of(this.parent);
    }

    public static class Builder {
        private final ListTemplate.Builder parentBuilder;
        private final String id;
        private final String name;
        private final Set<String> aliases = new HashSet<>();

        private String description = "";
        private int index = 0;
        @Nullable
        private Instant activeTo = null;

        public Builder(ListTemplate.Builder parentBuilder, String id, String name) {
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

        public Builder activeTo(Instant activeTo) {
            this.activeTo = activeTo;
            return this;
        }

        public Builder activeTo(Calendar activeTo) {
            this.activeTo = activeTo.toInstant();
            return this;
        }

        public Builder alias(String alias) {
            this.aliases.add(alias);
            return this;
        }

        public ListTemplate.Builder endEntry() {
            this.parentBuilder.addEntry(this.id, this);
            return this.parentBuilder;
        }

        public ListElementTemplate build(ComponentReference<ListTemplate> parent) {
            return new ListElementTemplate(
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
