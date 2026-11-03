package com.openrecordsmanager.api.template.relationship;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.TemplateComponent;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@JsonDeserialize
public record RelationshipTypeTemplate(
        ComponentReference<? extends TemplateComponent> sourceType,
        ComponentReference<? extends TemplateComponent> targetType,
        boolean uniquePerSource
) implements TemplateComponent {

    public RelationshipTypeTemplate {
        Objects.requireNonNull(sourceType, "Property 'sourceType' must not be null");
        Objects.requireNonNull(targetType, "Property 'targetType' must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Set<ComponentReference<? extends TemplateComponent>> getDependencies() {
        Set<ComponentReference<? extends TemplateComponent>> dependencies = new HashSet<>();
        dependencies.add(this.sourceType);
        dependencies.add(this.targetType);
        return dependencies;
    }

    public static final class Builder {
        private @Nullable ComponentReference<? extends TemplateComponent> sourceType;
        private @Nullable ComponentReference<? extends TemplateComponent> targetType;
        private boolean uniquePerSource = false;

        private Builder() {
        }

        public Builder sourceType(ComponentReference<? extends TemplateComponent> sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public Builder targetType(ComponentReference<? extends TemplateComponent> targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder uniquePerSource(boolean uniquePerSource) {
            this.uniquePerSource = uniquePerSource;
            return this;
        }

        public RelationshipTypeTemplate build() {
            return new RelationshipTypeTemplate(
                    Objects.requireNonNull(this.sourceType, "Property 'sourceType' must not be null"),
                    Objects.requireNonNull(this.targetType, "Property 'targetType' must not be null"),
                    this.uniquePerSource
            );
        }
    }
}
