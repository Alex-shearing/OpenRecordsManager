package com.openrecordsmanager.relationship;

import com.openrecordsmanager.property.ManagedObject;
import jakarta.persistence.*;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "relationship")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class Relationship {
    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, targetEntity = ManagedObject.class)
    @JoinColumn(name = "source_id", nullable = false)
    private ManagedObject<?> source;

    @ManyToOne(optional = false, fetch = FetchType.LAZY, targetEntity = ManagedObject.class)
    @JoinColumn(name = "target_id", nullable = false)
    private ManagedObject<?> target;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private RelationshipType type;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column
    @Nullable
    private Instant activeTo;

    @Deprecated
    protected Relationship() {
    }

    public Relationship(ManagedObject<?> source, ManagedObject<?> target, RelationshipType type) {
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.source = source;
        this.target = target;
        this.type = type;
        this.dateCreated = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public ManagedObject<?> getSource() {
        return this.source;
    }

    public ManagedObject<?> getTarget() {
        return this.target;
    }

    public RelationshipType getType() {
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
