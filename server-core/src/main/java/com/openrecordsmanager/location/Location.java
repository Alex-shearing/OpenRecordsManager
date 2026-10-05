package com.openrecordsmanager.location;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.type.LocationTypeProperty;
import com.openrecordsmanager.property.BuiltinProperty;
import com.openrecordsmanager.property.BuiltinPropertyBinding;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import jakarta.persistence.*;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "location")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "location_kind", discriminatorType = DiscriminatorType.STRING, length = 32)
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public abstract class Location extends ObjectPropertyHolder<Location, LocationPropertyValue> {
    public static final Map<ResourceIdentifier, BuiltinPropertyBinding<Location, ?>> BUILTIN_PROPERTY_BINDINGS =
            BuiltinPropertyBinding.scan(Location.class);

    @Id
    @BuiltinProperty(value = BuiltinPropertyIds.ID, readOnly = true)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private LocationType type;

    @BuiltinProperty(value = BuiltinPropertyIds.NAME, defaultSearch = true)
    @Column(nullable = false, unique = true)
    private String name;

    @BuiltinProperty(BuiltinPropertyIds.NOTES)
    @Column
    @Nullable
    private String notes;

    @BuiltinProperty(BuiltinPropertyIds.DATE_CREATED)
    @Column(nullable = false)
    private Instant dateCreated;

    @BuiltinProperty(value = BuiltinPropertyIds.DATE_MODIFIED, readOnly = true)
    @Column(nullable = false)
    private Instant dateModified;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "location_property_value",
            joinColumns = @JoinColumn(name = "location_id")
    )
    @MapKeyJoinColumn(name = "property_id")
    private Map<ObjectProperty<?>, LocationPropertyValue> properties = new HashMap<>();

    protected Location() {
    }

    protected Location(String name, LocationType type) {
        if (type.getKind() != this.getKind()) {
            throw new IllegalArgumentException(
                    "Location type " + type.getId() + " kind " + type.getKind()
                            + " does not match " + this.getKind()
            );
        }
        this.type = type;
        // Defaults before id/timestamps so readOnly builtins short-circuit on null→null.
        type.getProperties().forEach(p -> this.setPropertyFromJson(p.getProperty(), p.getDefault()));

        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.name = name;
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    public abstract LocationKind getKind();

    public UUID getId() {
        return this.id;
    }

    public LocationType getType() {
        return this.type;
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

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
    }

    @Override
    public boolean canSetProperty(ObjectProperty<?> property) {
        // Type schema properties, plus user-hidden plugin/system properties
        return this.type.hasProperty(property) || property.isUserHidden();
    }

    @Override
    public Set<ObjectProperty<?>> getPropertyKeys() {
        return this.type.getProperties().stream()
                .map(LocationTypeProperty::getProperty)
                .collect(Collectors.toSet());
    }

    @Override
    public LocationPropertyValue createProperty(ObjectProperty<?> property, @Nullable JsonNode value) {
        return new LocationPropertyValue(property, value);
    }

    @Override
    protected Map<ObjectProperty<?>, LocationPropertyValue> getDynamicProperties() {
        return this.properties;
    }

    @Override
    protected Map<ResourceIdentifier, BuiltinPropertyBinding<Location, ?>> getBuiltinPropertyBindings() {
        return BUILTIN_PROPERTY_BINDINGS;
    }

    @Override
    protected Location self() {
        return this;
    }
}
