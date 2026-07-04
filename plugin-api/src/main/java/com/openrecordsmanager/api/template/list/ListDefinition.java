package com.openrecordsmanager.api.template.list;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize
public record ListDefinition(String name, Map<String, ListElementDefinition> defaultEntries) implements Component {
    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, defaultEntries.size());
    }

    public static class Builder {
        private String name;
        private final HashMap<String, ListElementDefinition.Builder> defaultEntries = new HashMap<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public ListElementDefinition.Builder entry(String id, String name) {
            return new ListElementDefinition.Builder(this, id, name);
        }

        protected void addEntry(String id, ListElementDefinition.Builder defaultEntry) {
            this.defaultEntries.put(id, defaultEntry);
        }

        public ListDefinition build() {
            ListDefinition parent = new ListDefinition(this.name, new HashMap<>());

            ComponentReference<ListDefinition> parentRef = ComponentReference.of(parent);

            ImmutableMap<String, ListElementDefinition> entries = this.defaultEntries.entrySet().stream()
                    .map(builder -> Map.entry(builder.getKey(), builder.getValue().build(parentRef)))
                    .sorted(Comparator.comparingInt(o -> o.getValue().index()))
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

            parent.defaultEntries.putAll(entries);

            return parent;
        }
    }
}
