package com.openrecordsmanager.location.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.template.location.LocationTypeTemplate;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.template.ManagedObjectType;
import com.openrecordsmanager.template.ManagedObjectTypeProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "location_type")
@DiscriminatorValue("location_type")
@PrimaryKeyJoinColumn(name = "id")
public class LocationType extends ManagedObjectType {
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    @Nullable
    private ManagedObjectKind kind;

    /**
     * Subselect (not join) so an EAGER load does not multiply rows against {@code baseType}'s
     * properties and inflate this set under flush.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(
            name = "location_type_property",
            joinColumns = @JoinColumn(name = "location_type")
    )
    private Set<ManagedObjectTypeProperty<?>> properties = new HashSet<>();

    @Deprecated
    protected LocationType() {
    }

    public LocationType(
            ResourceIdentifier id,
            @Nullable ManagedObjectKind kind,
            Set<ManagedObjectTypeProperty<?>> properties
    ) {
        this(id, kind, false, null, properties);
    }

    public LocationType(
            ResourceIdentifier id,
            @Nullable ManagedObjectKind kind,
            boolean abstractType,
            @Nullable ManagedObjectType baseType,
            Set<ManagedObjectTypeProperty<?>> properties
    ) {
        super(id, abstractType, baseType);
        LocationTypeTemplate.validateKind(kind, abstractType);
        this.kind = kind;
        this.properties = properties;
    }

    @Override
    public @Nullable ManagedObjectKind getKind() {
        return this.kind;
    }

    @Override
    public Set<ManagedObjectTypeProperty<?>> getDeclaredProperties() {
        return this.properties;
    }

    public void setKind(@Nullable ManagedObjectKind kind) {
        LocationTypeTemplate.validateKind(kind, this.isAbstract());
        this.kind = kind;
        this.touchDateModified();
    }

    public void setProperties(Set<ManagedObjectTypeProperty<?>> properties) {
        this.properties = properties;
        this.touchDateModified();
    }

    @Override
    public void setAbstractType(boolean abstractType) {
        LocationTypeTemplate.validateKind(this.kind, abstractType);
        super.setAbstractType(abstractType);
    }

    @Override
    public ComponentType<?> getComponentType() {
        return ComponentTypes.LOCATION_TYPE;
    }
}
