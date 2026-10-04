package com.openrecordsmanager.location.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.template.RegisteredComponent;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "location_type")
@SuppressWarnings("NotNullFieldNotInitialized")
public class LocationType implements RegisteredComponent {
    @Id
    @JavaType(ResourceIdentifierJavaType.class)
    private ResourceIdentifier id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LocationKind kind;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column(nullable = false)
    private Instant dateModified;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "location_type_property",
            joinColumns = @JoinColumn(name = "location_type")
    )
    private Set<LocationTypeProperty<?>> properties = new HashSet<>();

    @Deprecated
    protected LocationType() {
    }

    public LocationType(
            ResourceIdentifier id,
            LocationKind kind,
            Set<LocationTypeProperty<?>> properties
    ) {
        if (kind == LocationKind.ANY) {
            throw new IllegalArgumentException("Location type kind must be USER or GROUP");
        }
        this.id = id;
        this.kind = kind;
        this.properties = properties;
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    public ResourceIdentifier getId() {
        return this.id;
    }

    public LocationKind getKind() {
        return this.kind;
    }

    public Set<LocationTypeProperty<?>> getProperties() {
        return this.properties;
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

    public boolean hasProperty(ObjectProperty<?> property) {
        return this.properties.stream()
                .anyMatch(prop -> Objects.equals(prop.getProperty(), property));
    }

    @Override
    public ComponentType<?> getComponentType() {
        return ComponentTypes.LOCATION_TYPE;
    }
}
