package com.openrecordsmanager.template;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.database.util.ResourceIdentifierJavaType;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "registered_component")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "component_kind", discriminatorType = DiscriminatorType.STRING, length = 64)
@SuppressWarnings("NotNullFieldNotInitialized")
public abstract class RegisteredComponent {
    @Id
    @JavaType(ResourceIdentifierJavaType.class)
    private ResourceIdentifier id;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column(nullable = false)
    private Instant dateModified;

    @Deprecated
    protected RegisteredComponent() {
    }

    protected RegisteredComponent(ResourceIdentifier id) {
        this.id = id;
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
    }

    public abstract ComponentType<?> getComponentType();

    public ResourceIdentifier getId() {
        return this.id;
    }

    public ComponentReference<?> getReference() {
        return ComponentReference.of(this.getComponentType(), this.getId());
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

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        RegisteredComponent that = (RegisteredComponent) o;
        return Objects.equals(this.getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }
}
