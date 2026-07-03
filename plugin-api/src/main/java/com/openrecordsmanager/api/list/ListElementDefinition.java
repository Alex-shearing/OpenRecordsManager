package com.openrecordsmanager.api.list;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import org.jspecify.annotations.Nullable;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public record ListElementDefinition(
        String display,
        String description,
        int index,
        @Nullable Date activeTo,
        Set<String> aliases,
        ComponentReference<ListDefinition> parent
) implements Component {

    @Override
    public Set<ComponentReference<? extends Component>> getDependencies() {
        return Set.of(this.parent);
    }

    public static class Builder {
        private final ListDefinition.Builder parentBuilder;
        private final String id;
        private final String display;
        private final Set<String> aliases = new HashSet<>();

        private String description = "";
        private int index = 0;
        private Date activeTo = null;

        public Builder(ListDefinition.Builder parentBuilder, String id, String display) {
            this.parentBuilder = parentBuilder;
            this.id = id;
            this.display = display;
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
                    this.display,
                    this.description,
                    this.index,
                    this.activeTo,
                    this.aliases,
                    parent
            );
        }
    }
}
