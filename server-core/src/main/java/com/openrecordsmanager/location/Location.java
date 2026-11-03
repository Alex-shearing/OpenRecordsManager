package com.openrecordsmanager.location;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.property.BuiltinProperty;
import com.openrecordsmanager.property.BuiltinPropertyBinding;
import com.openrecordsmanager.property.ManagedObject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.Map;

@Entity
@Table(name = "location")
@PrimaryKeyJoinColumn(name = "id")
@SuppressWarnings("CanBeFinal")
public abstract class Location extends ManagedObject<LocationType> {
    public static final Map<ResourceIdentifier, BuiltinPropertyBinding<?>> BUILTIN_PROPERTY_BINDINGS =
            BuiltinPropertyBinding.scan(Location.class);

    @BuiltinProperty(value = BuiltinPropertyIds.NAME, defaultSearch = true)
    @Column(nullable = false, unique = true)
    private String name;

    @Deprecated
    protected Location() {
    }

    protected Location(String name, LocationType type) {
        super(type);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.touchDateModified();
    }

    /**
     * Human-facing label for this location.
     */
    public String getDisplayName() {
        return this.getName();
    }
}
