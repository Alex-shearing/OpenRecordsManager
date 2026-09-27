package com.openrecordsmanager.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.property.*;
import jakarta.persistence.*;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "user_details")
@SuppressWarnings({"NotNullFieldNotInitialized", "CanBeFinal"})
public class User extends ObjectPropertyHolder<User, UserPropertyValue> implements UserDetails {
    public static final Map<ResourceIdentifier, BuiltinPropertyBinding<User, ?>> BUILTIN_PROPERTY_BINDINGS =
            BuiltinPropertyBinding.scan(User.class);

    @Id
    private UUID id;

    @BuiltinProperty(value = BuiltinPropertyIds.USERNAME_ID, defaultSearch = true)
    @Column(unique = true, nullable = false)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @Nullable
    private AuthProvider authProvider;

    @BuiltinProperty(BuiltinPropertyIds.DATE_CREATED_ID)
    @Column(nullable = false)
    private Instant dateCreated;

    @BuiltinProperty(value = BuiltinPropertyIds.DATE_MODIFIED_ID, readOnly = true)
    @Column(nullable = false)
    private Instant dateModified;

    @BuiltinProperty(value = BuiltinPropertyIds.GIVEN_NAME_ID, defaultSearch = true)
    @Column
    @Nullable
    private String givenName;

    @BuiltinProperty(value = BuiltinPropertyIds.SURNAME_ID, defaultSearch = true)
    @Column
    @Nullable
    private String surname;

    @BuiltinProperty(BuiltinPropertyIds.HONORIFIC_ID)
    @Column
    @Nullable
    private String honorific;

    @BuiltinProperty(value = BuiltinPropertyIds.EMAIL_ID, defaultSearch = true)
    @Column
    @Nullable
    private String email;

    @BuiltinProperty(BuiltinPropertyIds.NOTES_ID)
    @Column
    @Nullable
    private String notes;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "session_epoch", nullable = false)
    private int sessionEpoch = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_property_value",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @MapKeyJoinColumn(name = "property_id")
    private Map<ObjectProperty<?>, UserPropertyValue> properties = new HashMap<>();

    @Deprecated
    protected User() {
    }

    public User(String username, @Nullable AuthProvider authProvider) {
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
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

    public int getSessionEpoch() {
        return this.sessionEpoch;
    }

    public void bumpSessionEpoch() {
        this.sessionEpoch++;
    }

    @Override
    public boolean canSetProperty(ObjectProperty<?> property) {
        return true;
    }

    @Override
    public Set<ObjectProperty<?>> getPropertyKeys() {
        ObjectPropertyLookup lookup = ObjectPropertyLookup.requireInstalled();
        Set<ObjectProperty<?>> keys = new LinkedHashSet<>();
        for (ResourceIdentifier id : BUILTIN_PROPERTY_BINDINGS.keySet()) {
            keys.add(lookup.require(id));
        }
        keys.addAll(this.properties.keySet());
        return keys;
    }

    @Override
    public UserPropertyValue createProperty(ObjectProperty<?> property, @Nullable JsonNode value) {
        return new UserPropertyValue(property, value);
    }

    @Override
    protected Map<ObjectProperty<?>, UserPropertyValue> getDynamicProperties() {
        return this.properties;
    }

    @Override
    protected Map<ResourceIdentifier, BuiltinPropertyBinding<User, ?>> getBuiltinPropertyBindings() {
        return BUILTIN_PROPERTY_BINDINGS;
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
