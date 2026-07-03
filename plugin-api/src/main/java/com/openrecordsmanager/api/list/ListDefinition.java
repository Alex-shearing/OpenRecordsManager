package com.openrecordsmanager.api.list;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.api.Component;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(builder = ListDefinition.Builder.class)
public class ListDefinition implements Component {
    public final String display;
    public final Map<String, ListElementDefinition> defaultEntries;

    private ListDefinition(String display, Map<String, ListElementDefinition> defaultEntries) {
        this.display = display;
        this.defaultEntries = defaultEntries;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ListDefinition that)) return false;
        return Objects.equals(display, that.display) &&
                Objects.equals(defaultEntries.size(), that.defaultEntries.size());
    }

    @Override
    public int hashCode() {
        return Objects.hash(display, defaultEntries.size());
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final HashMap<String, ListElementDefinition.Builder> defaultEntries = new HashMap<>();
        private String name;

        private Builder(String name) {
            this.name = name;
        }

        @JsonCreator
        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public ListElementDefinition.Builder entry(String id, String display) {
            return new ListElementDefinition.Builder(this, id, display);
        }

        protected void addEntry(String id, ListElementDefinition.Builder defaultEntry) {
            this.defaultEntries.put(id, defaultEntry);
        }

        public ListDefinition build() {
            ListDefinition parent = new ListDefinition(this.name, new HashMap<>());

            ImmutableMap<String, ListElementDefinition> entries = this.defaultEntries.entrySet().stream()
                    .map(builder -> Map.entry(builder.getKey(), builder.getValue().build(parent)))
                    .sorted(Comparator.comparingInt(o -> o.getValue().index()))
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

            parent.defaultEntries.putAll(entries);

            return parent;
        }
    }
}
