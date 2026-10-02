package com.openrecordsmanager.location.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import com.openrecordsmanager.template.RegisteredComponent;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;

import java.time.Instant;

@Entity
@Table(name = "location_relationship_type")
@SuppressWarnings("NotNullFieldNotInitialized")
public class LocationRelationshipType implements RegisteredComponent {
    @Id
    @JavaType(ResourceIdentifierJavaType.class)
    private ResourceIdentifier id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 32)
    private LocationKind sourceKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_kind", nullable = false, length = 32)
    private LocationKind targetKind;

    @Column(name = "unique_per_source", nullable = false)
    private boolean uniquePerSource;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column(nullable = false)
    private Instant dateModified;

    @Deprecated
    protected LocationRelationshipType() {
    }

    public LocationRelationshipType(
            ResourceIdentifier id,
            LocationKind sourceKind,
            LocationKind targetKind,
            boolean uniquePerSource
    ) {
        this.id = id;
        this.sourceKind = sourceKind;
        this.targetKind = targetKind;
        this.uniquePerSource = uniquePerSource;
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    @Override
    public ComponentType<?> getComponentType() {
        return ComponentTypes.LOCATION_RELATIONSHIP_TYPE;
    }

    @Override
    public ResourceIdentifier getId() {
        return this.id;
    }

    public LocationKind getSourceKind() {
        return this.sourceKind;
    }

    public LocationKind getTargetKind() {
        return this.targetKind;
    }

    public boolean isUniquePerSource() {
        return this.uniquePerSource;
    }

    public void setSourceKind(LocationKind sourceKind) {
        this.sourceKind = sourceKind;
        this.touchDateModified();
    }

    public void setTargetKind(LocationKind targetKind) {
        this.targetKind = targetKind;
        this.touchDateModified();
    }

    public void setUniquePerSource(boolean uniquePerSource) {
        this.uniquePerSource = uniquePerSource;
        this.touchDateModified();
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
    }
}
