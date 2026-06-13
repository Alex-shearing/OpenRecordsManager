package com.openrecordsmanager.list;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public record ListItem(String display, String description, int index, Date activeTo, Set<String> aliases) {

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String display;
        private final Set<String> aliases = new HashSet<>();
        private String description = "";
        private int index = 0;
        private Date activeTo;

        public Builder(String display) {
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

        public ListItem build() {
            return new ListItem(display, description, this.index, activeTo, aliases);
        }
    }
}
