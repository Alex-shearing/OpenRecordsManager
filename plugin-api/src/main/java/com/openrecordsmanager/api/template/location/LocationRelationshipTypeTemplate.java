package com.openrecordsmanager.api.template.location;

import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.template.TemplateComponent;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

@JsonDeserialize
public record LocationRelationshipTypeTemplate(
        LocationKind sourceKind,
        LocationKind targetKind,
        boolean uniquePerSource
) implements TemplateComponent {

    public LocationRelationshipTypeTemplate {
        Objects.requireNonNull(sourceKind, "Property 'sourceKind' must not be null");
        Objects.requireNonNull(targetKind, "Property 'targetKind' must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private LocationKind sourceKind = LocationKind.ANY;
        private LocationKind targetKind = LocationKind.ANY;
        private boolean uniquePerSource = false;

        private Builder() {
        }

        public Builder sourceKind(LocationKind sourceKind) {
            this.sourceKind = sourceKind;
            return this;
        }

        public Builder targetKind(LocationKind targetKind) {
            this.targetKind = targetKind;
            return this;
        }

        public Builder uniquePerSource(boolean uniquePerSource) {
            this.uniquePerSource = uniquePerSource;
            return this;
        }

        public LocationRelationshipTypeTemplate build() {
            return new LocationRelationshipTypeTemplate(this.sourceKind, this.targetKind, this.uniquePerSource);
        }
    }
}
