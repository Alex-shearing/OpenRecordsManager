package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
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
    @JsonProperty
    public Set<UserPropertyValue<?>> properties;

    @Deprecated
    protected User() {
    }

    public User(UUID id) {
        this.id = id;
        this.properties = new HashSet<>();
    }

    @Override
    public Set<UserPropertyValue<?>> getProperties() {
        return this.properties;
    }
}
