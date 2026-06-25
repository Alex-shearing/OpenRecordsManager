package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user")
public class User implements ObjectPropertyHolder<UserPropertyValue<?>> {
    @Id
    @JsonProperty
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @Nullable
    public AuthProvider authProvider;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    @MapKey(name = "property")
    @JsonProperty
    private Map<ObjectProperty<?>, UserPropertyValue<?>> properties;

    @Deprecated
    protected User() {
    }

    public User(UUID id) {
        this.id = id;
        this.properties = new HashMap<>();
    }

    @Override
    public Map<ObjectProperty<?>, UserPropertyValue<?>> getProperties() {
        return this.properties;
    }

    @Override
    public boolean canSetProperty(ObjectProperty<?> property) {
        return true;
    }

    @Override
    public <V> UserPropertyValue<?> createProperty(ObjectProperty<V> property, V value) {
        return new UserPropertyValue<>(this, property, value);
    }
}
