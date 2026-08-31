package com.openrecordsmanager.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinProperties;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.property.BuiltinPropertyMapper;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.property.ObjectPropertyHolder;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "user_details")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class User extends ObjectPropertyHolder<User, UserPropertyValue<?>> implements UserDetails {
    private static final Map<ResourceIdentifier, BuiltinPropertyMapper<User, ?>> BUILTIN_PROPERTY_MAPPERS = Map.of(
            BuiltinProperties.DATE_CREATED_ID, BuiltinPropertyMapper.of(
                    User::getDateCreated,
                    (u, v) -> u.dateCreated = Objects.requireNonNull(v)
            ),
            BuiltinProperties.DATE_MODIFIED_ID, BuiltinPropertyMapper.of(
                    User::getDateModified,
                    (_, _) -> {
                        throw new IllegalArgumentException("date modified cannot be set explicitly");
                    }
            ),
            BuiltinProperties.GIVEN_NAME_ID, BuiltinPropertyMapper.of(
                    User::getGivenName,
                    (u, v) -> u.givenName = v
            ),
            BuiltinProperties.SURNAME_ID, BuiltinPropertyMapper.of(
                    User::getSurname,
                    (u, v) -> u.surname = v
            ),
            BuiltinProperties.HONORIFIC_ID, BuiltinPropertyMapper.of(
                    User::getHonorific,
                    (u, v) -> u.honorific = v
            ),
            BuiltinProperties.EMAIL_ID, BuiltinPropertyMapper.of(
                    User::getEmail,
                    (u, v) -> u.email = v
            ),
            BuiltinProperties.NOTES_ID, BuiltinPropertyMapper.of(
                    User::getNotes,
                    (u, v) -> u.notes = v
            )
    );

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @Nullable
    private AuthProvider authProvider;

    @Column(nullable = false)
    private Instant dateCreated;

    @Column(nullable = false)
    private Instant dateModified;

    @Column
    @Nullable
    private String givenName;

    @Column
    @Nullable
    private String surname;

    @Column
    @Nullable
    private String honorific;

    @Column
    @Nullable
    private String email;

    @Column
    @Nullable
    private String notes;

    @Column(nullable = false)
    private boolean enabled = true;

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
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
        this.enabled = true;
    }

    public UUID getId() {
        return this.id;
    }

    public @Nullable AuthProvider getAuthProvider() {
        return this.authProvider;
    }

    public void setUsername(String username) {
        this.username = username;
        this.touchDateModified();
    }

    public void setAuthProvider(@Nullable AuthProvider authProvider) {
        this.authProvider = authProvider;
        this.touchDateModified();
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

    public @Nullable String getGivenName() {
        return this.givenName;
    }

    public @Nullable String getSurname() {
        return this.surname;
    }

    public @Nullable String getHonorific() {
        return this.honorific;
    }

    public @Nullable String getEmail() {
        return this.email;
    }

    public @Nullable String getNotes() {
        return this.notes;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.touchDateModified();
    }

    @Override
    public boolean canSetProperty(ObjectProperty<?> property) {
        return true;
    }

    @Override
    public Set<ObjectProperty<?>> getPropertyKeys() {
        Set<ObjectProperty<?>> keys = new LinkedHashSet<>(UserBuiltinColumnPropertyRegistry.userColumnPropertyKeys());
        keys.addAll(this.properties.keySet());
        return keys;
    }

    @Override
    public <V> UserPropertyValue<V> createProperty(ObjectProperty<V> property, @Nullable V value) {
        return new UserPropertyValue<>(this, property, value);
    }

    @Override
    protected Map<ObjectProperty<?>, UserPropertyValue<?>> getDynamicProperties() {
        return this.properties;
    }

    @Override
    protected Map<ResourceIdentifier, BuiltinPropertyMapper<User, ?>> getBuiltinPropertyMappers() {
        return BUILTIN_PROPERTY_MAPPERS;
    }

    @Override
    protected User self() {
        return this;
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
