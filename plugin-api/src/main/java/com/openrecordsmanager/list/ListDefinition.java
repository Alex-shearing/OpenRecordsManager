package com.openrecordsmanager.list;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.RegisterableComponent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ListDefinition implements RegisterableComponent {
    public final String id;
    public final String display;
    public final ImmutableMap<String, ListItem> defaultEntries;

    private ListDefinition(String id, String display, ImmutableMap<String, ListItem> defaultEntries) {
        this.id = id;
        this.display = display;
        this.defaultEntries = defaultEntries;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    @Override
    public String id() {
        return this.id;
    }

    public static class Builder {
        private final String id;
        private final HashMap<String, ListItem> defaultEntries = new HashMap<>();
        private String name;

        Builder(String id) {
            this.id = id;
            this.name = id;
        }

        public Builder display(String display) {
            this.name = display;
            return this;
        }

        public Builder entry(String id, ListItem defaultEntry) {
            this.defaultEntries.put(id, defaultEntry);
            return this;
        }

        public ListDefinition build() {
            ImmutableMap<String, ListItem> entries = this.defaultEntries.entrySet().stream()
                    .sorted(Comparator.comparingInt(o -> o.getValue().index()))
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
            return new ListDefinition(this.id, this.name, entries);
        }
    }
}
