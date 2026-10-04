package com.openrecordsmanager.location.group;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.location.Location;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.property.BuiltinPropertyBinding;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.Map;

@Entity
@Table(name = "group_details")
@DiscriminatorValue("GROUP")
@PrimaryKeyJoinColumn(name = "id")
public class Group extends Location {
    public static final Map<ResourceIdentifier, BuiltinPropertyBinding<Location, ?>> BUILTIN_PROPERTY_BINDINGS =
            BuiltinPropertyBinding.scan(Group.class);

    @Deprecated
    protected Group() {
    }

    public Group(String name, LocationType type) {
        super(name, type);
    }

    @Override
    public LocationKind getKind() {
        return LocationKind.GROUP;
    }

    @Override
    protected Map<ResourceIdentifier, BuiltinPropertyBinding<Location, ?>> getBuiltinPropertyBindings() {
        return BUILTIN_PROPERTY_BINDINGS;
    }
}
