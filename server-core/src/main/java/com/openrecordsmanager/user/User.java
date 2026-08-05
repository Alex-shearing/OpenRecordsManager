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

    @Column()
    public String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @Nullable
    public AuthProvider authProvider;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    @MapKey(name = "property")
    private Map<ObjectProperty<?>, UserPropertyValue<?>> properties;

    @Deprecated
    protected User() {
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
    public <V> UserPropertyValue<?> createProperty(ObjectProperty<V> property, V value) {
        return new UserPropertyValue<>(this, property, value);
    }

    @Override
    @Transient
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @Nullable String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return "";
    }
}
