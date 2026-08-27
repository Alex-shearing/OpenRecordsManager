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
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class User implements ObjectPropertyHolder<UserPropertyValue<?>>, UserDetails {
    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @Nullable
    private AuthProvider authProvider;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user", fetch = FetchType.EAGER)
    @MapKey(name = "property")
    private Map<ObjectProperty<?>, UserPropertyValue<?>> properties = new HashMap<>();

    @Deprecated
    protected User() {
    }

    public User(String username, @Nullable AuthProvider authProvider) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.authProvider = authProvider;
    }

    public UUID getId() {
        return this.id;
    }

    public @Nullable AuthProvider getAuthProvider() {
        return this.authProvider;
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
    public <V> UserPropertyValue<V> createProperty(ObjectProperty<V> property, @Nullable V value) {
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
