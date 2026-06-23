package com.openrecordsmanager.api.list;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.api.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class ListDefinition implements Component {
    public final String id;
    public final String display;
    public final Map<String, ListElementDefinition> defaultEntries;

    private ListDefinition(String id, String display, Map<String, ListElementDefinition> defaultEntries) {
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
        private final HashMap<String, ListElementDefinition.Builder> defaultEntries = new HashMap<>();
        private String display;

        private Builder(String id) {
            this.id = id;
            this.display = id;
        }

        public Builder display(String display) {
            this.display = display;
            return this;
        }

        public ListElementDefinition.Builder entry(String id, String display) {
            return new ListElementDefinition.Builder(this, id, display);
        }

        protected void addEntry(String id, ListElementDefinition.Builder defaultEntry) {
            this.defaultEntries.put(id, defaultEntry);
        }

        public ListDefinition build() {
            ListDefinition parent = new ListDefinition(this.id, this.display, new HashMap<>());

            ImmutableMap<String, ListElementDefinition> entries = this.defaultEntries.entrySet().stream()
                    .map(builder -> Map.entry(builder.getKey(), builder.getValue().build(parent)))
                    .sorted(Comparator.comparingInt(o -> o.getValue().index()))
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
            parent.defaultEntries.putAll(entries);

            return parent;
        }
    }
}
