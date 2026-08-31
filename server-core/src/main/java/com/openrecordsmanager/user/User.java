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
public class User implements ObjectPropertyHolder<UserPropertyValue<?>>, UserDetails {
    private static final Map<ResourceIdentifier, BuiltinPropertyMapper<User, ?>> BUILTIN_PROPERTY_MAPPERS = Map.of(
            BuiltinProperties.DATE_CREATED_ID, BuiltinPropertyMapper.of(User::getDateCreated, (user, v) -> user.setDateCreated(Objects.requireNonNull(v))),
            BuiltinProperties.DATE_MODIFIED_ID, BuiltinPropertyMapper.of(User::getDateModified, (user, v) -> user.setDateModified(Objects.requireNonNull(v))),
            BuiltinProperties.GIVEN_NAME_ID, BuiltinPropertyMapper.of(User::getGivenName, User::setGivenName),
            BuiltinProperties.SURNAME_ID, BuiltinPropertyMapper.of(User::getSurname, User::setSurname),
            BuiltinProperties.HONORIFIC_ID, BuiltinPropertyMapper.of(User::getHonorific, User::setHonorific),
            BuiltinProperties.EMAIL_ID, BuiltinPropertyMapper.of(User::getEmail, User::setEmail),
            BuiltinProperties.NOTES_ID, BuiltinPropertyMapper.of(User::getNotes, User::setNotes)
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
    }

    public void setAuthProvider(@Nullable AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    public Instant getDateCreated() {
        return this.dateCreated;
    }

    public void setDateCreated(Instant dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Instant getDateModified() {
        return this.dateModified;
    }

    public void setDateModified(Instant dateModified) {
        this.dateModified = dateModified;
    }

    public @Nullable String getGivenName() {
        return this.givenName;
    }

    public void setGivenName(@Nullable String givenName) {
        this.givenName = givenName;
    }

    public @Nullable String getSurname() {
        return this.surname;
    }

    public void setSurname(@Nullable String surname) {
        this.surname = surname;
    }

    public @Nullable String getHonorific() {
        return this.honorific;
    }

    public void setHonorific(@Nullable String honorific) {
        this.honorific = honorific;
    }

    public @Nullable String getEmail() {
        return this.email;
    }

    public void setEmail(@Nullable String email) {
        this.email = email;
    }

    public @Nullable String getNotes() {
        return this.notes;
    }

    public void setNotes(@Nullable String notes) {
        this.notes = notes;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void touchDateModified() {
        this.dateModified = Instant.now();
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
    public <K> @Nullable K getProperty(ObjectProperty<K> property) {
        BuiltinPropertyMapper<User, ?> builtinMapper = BUILTIN_PROPERTY_MAPPERS.get(property.getId());

        if (builtinMapper != null) {
            return property.getType().cast(builtinMapper.get(this));
        }

        UserPropertyValue<?> value = this.properties.get(property);
        if (value == null) {
            return null;
        }
        return property.getType().cast(value.getValue());
    }

    @Override
    public <K> void setProperty(ObjectProperty<K> property, @Nullable K value) {
        BuiltinPropertyMapper<User, ?> builtinMapper = BUILTIN_PROPERTY_MAPPERS.get(property.getId());

        if (builtinMapper != null) {
            builtinMapper.set(this, value);
            return;
        }

        UserPropertyValue<?> holder = this.properties.get(property);
        if (holder == null) {
            holder = this.createProperty(property, value);
            this.properties.put(property, holder);
        }

        holder.setValueUntyped(value);
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
