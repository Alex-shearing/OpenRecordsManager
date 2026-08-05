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
public record ListTemplate(String name, Map<String, ListElementTemplate> defaultEntries) implements Component {
    public ListTemplate {
        Objects.requireNonNull(name, "Property 'name' must not be null");
        Objects.requireNonNull(defaultEntries, "Property 'defaultEntries' must not be null");
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, defaultEntries.size());
    }

    public static class Builder {
        private String name;
        private final HashMap<String, ListElementTemplate.Builder> defaultEntries = new HashMap<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public ListElementTemplate.Builder entry(String id, String name) {
            return new ListElementTemplate.Builder(this, id, name);
        }

        protected void addEntry(String id, ListElementTemplate.Builder defaultEntry) {
            this.defaultEntries.put(id, defaultEntry);
        }

        public ListTemplate build() {
            ListTemplate parent = new ListTemplate(this.name, new HashMap<>());

            ComponentReference<ListTemplate> parentRef = ComponentReference.of(parent);

            ImmutableMap<String, ListElementTemplate> entries = this.defaultEntries.entrySet().stream()
                    .map(builder -> Map.entry(builder.getKey(), builder.getValue().build(parentRef)))
                    .sorted(Comparator.comparingInt(o -> o.getValue().index()))
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

            parent.defaultEntries.putAll(entries);

            return parent;
        }
    }
}
