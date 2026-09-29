package com.openrecordsmanager.api.template.list;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.TemplateComponent;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@JsonDeserialize
public record ListTemplate(Map<String, ListElementTemplate> defaultEntries) implements TemplateComponent {
    public ListTemplate {
        Objects.requireNonNull(defaultEntries, "Property 'defaultEntries' must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultEntries.size());
    }

    public static class Builder {
        private final HashMap<String, ListElementTemplate.Builder> defaultEntries = new HashMap<>();

        private Builder() {
        }

        public Builder entry(String id, @Nullable Consumer<ListElementTemplate.Builder> builder) {
            ListElementTemplate.Builder elementBuilder = new ListElementTemplate.Builder();
            if (builder != null) {
                builder.accept(elementBuilder);
            }
            this.addEntry(id, elementBuilder);
            return this;
        }

        public Builder entry(String id) {
            return this.entry(id, null);
        }

        protected void addEntry(String id, ListElementTemplate.Builder defaultEntry) {
            this.defaultEntries.put(id, defaultEntry);
        }

        public ListTemplate build() {
            ListTemplate parent = new ListTemplate(new HashMap<>());

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
