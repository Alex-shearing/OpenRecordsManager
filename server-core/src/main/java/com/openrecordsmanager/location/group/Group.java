package com.openrecordsmanager.location.group;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.property.ManagedObjectKind;
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
    public static final Map<ResourceIdentifier, BuiltinPropertyBinding<?>> BUILTIN_PROPERTY_BINDINGS =
            BuiltinPropertyBinding.scan(Group.class);

    @Deprecated
    protected Group() {
    }

    public Group(String name, LocationType type) {
        super(name, type);
    }

    @Override
    public ManagedObjectKind getKind() {
        return ManagedObjectKind.GROUP;
    }

    @Override
    protected Map<ResourceIdentifier, BuiltinPropertyBinding<?>> getBuiltinPropertyBindings() {
        return BUILTIN_PROPERTY_BINDINGS;
    }
}
