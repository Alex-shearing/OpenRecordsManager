package com.openrecordsmanager.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.template.ManagedObjectType;
import com.openrecordsmanager.template.RegisteredComponent;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "relationship_type")
@DiscriminatorValue("relationship_type")
@PrimaryKeyJoinColumn(name = "id")
@SuppressWarnings("NotNullFieldNotInitialized")
public class RelationshipType extends RegisteredComponent {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "source_type_id", nullable = false)
    private ManagedObjectType sourceType;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_type_id", nullable = false)
    private ManagedObjectType targetType;

    @Column(name = "unique_per_source", nullable = false)
    private boolean uniquePerSource;

    @Deprecated
    protected RelationshipType() {
    }

    public RelationshipType(
            ResourceIdentifier id,
            ManagedObjectType sourceType,
            ManagedObjectType targetType,
            boolean uniquePerSource
    ) {
        super(id);
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.uniquePerSource = uniquePerSource;
    }

    @Override
    public ComponentType<?> getComponentType() {
        return ComponentTypes.RELATIONSHIP_TYPE;
    }

    public ManagedObjectType getSourceType() {
        return this.sourceType;
    }

    public ManagedObjectType getTargetType() {
        return this.targetType;
    }

    public ResourceIdentifier getSourceTypeId() {
        return this.sourceType.getId();
    }

    public ResourceIdentifier getTargetTypeId() {
        return this.targetType.getId();
    }

    public @Nullable ManagedObjectKind getSourceKind() {
        return this.sourceType.getKind();
    }

    public @Nullable ManagedObjectKind getTargetKind() {
        return this.targetType.getKind();
    }

    public boolean isUniquePerSource() {
        return this.uniquePerSource;
    }

    public void setSourceType(ManagedObjectType sourceType) {
        this.sourceType = sourceType;
        this.touchDateModified();
    }

    public void setTargetType(ManagedObjectType targetType) {
        this.targetType = targetType;
        this.touchDateModified();
    }

    public void setUniquePerSource(boolean uniquePerSource) {
        this.uniquePerSource = uniquePerSource;
        this.touchDateModified();
    }
}
