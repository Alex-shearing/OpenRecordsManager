package com.openrecordsmanager.user;

import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Table(name = "user_details")
public class User implements ObjectPropertyHolder<UserPropertyValue<?>>, UserDetails {
    @Id
    public UUID id;

    @Column(unique = true, nullable = false)
    public String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @Nullable
    public AuthProvider authProvider;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user", fetch = FetchType.EAGER)
    @MapKey(name = "property")
    private Map<ObjectProperty<?>, UserPropertyValue<?>> properties;

    @Deprecated
    protected User() {
        this.properties = new HashMap<>();
    }

    public User(UUID id, String username) {
        this.id = id;
        this.username = username;
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
    public <V> UserPropertyValue<V> createProperty(ObjectProperty<V> property, V value) {
        return new UserPropertyValue<>(this, property, value);
    }

    @Override
    @Transient
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @Nullable String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.username;
    }
}
