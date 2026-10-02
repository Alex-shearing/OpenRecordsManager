package com.openrecordsmanager.location.relationship;

import com.openrecordsmanager.location.Location;
import jakarta.persistence.*;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "location_relationship")
public class LocationRelationship {
    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Location source;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private Location target;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private LocationRelationshipType type;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column
    @Nullable
    private Instant activeTo;

    @Deprecated
    protected LocationRelationship() {
    }

    public LocationRelationship(Location source, Location target, LocationRelationshipType type) {
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.source = source;
        this.target = target;
        this.type = type;
        this.dateCreated = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public Location getSource() {
        return this.source;
    }

    public Location getTarget() {
        return this.target;
    }

    public LocationRelationshipType getType() {
        return this.type;
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public @Nullable Instant getActiveTo() {
        return this.activeTo;
    }

    public boolean isActive() {
        return this.activeTo == null;
    }

    public void end(Instant activeTo) {
        this.activeTo = activeTo;
    }
}
