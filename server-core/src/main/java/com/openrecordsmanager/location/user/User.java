package com.openrecordsmanager.location.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.location.Location;
import com.openrecordsmanager.property.BuiltinProperty;
import com.openrecordsmanager.property.BuiltinPropertyBinding;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "user_details")
@DiscriminatorValue("USER")
@PrimaryKeyJoinColumn(name = "id")
@SuppressWarnings({"CanBeFinal"})
public class User extends Location implements UserDetails {
    public static final Map<ResourceIdentifier, BuiltinPropertyBinding<Location, ?>> BUILTIN_PROPERTY_BINDINGS =
            BuiltinPropertyBinding.scan(User.class);

    @BuiltinProperty(value = BuiltinPropertyIds.USERNAME, defaultSearch = true)
    @Column(unique = true, nullable = false)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @Nullable
    private AuthProvider authProvider;

    @BuiltinProperty(value = BuiltinPropertyIds.GIVEN_NAME, defaultSearch = true)
    @Column
    @Nullable
    private String givenName;

    @BuiltinProperty(value = BuiltinPropertyIds.SURNAME, defaultSearch = true)
    @Column
    @Nullable
    private String surname;

    @BuiltinProperty(BuiltinPropertyIds.HONORIFIC)
    @Column
    @Nullable
    private String honorific;

    @BuiltinProperty(value = BuiltinPropertyIds.EMAIL, defaultSearch = true)
    @Column
    @Nullable
    private String email;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "session_epoch", nullable = false)
    private int sessionEpoch = 0;

    @Deprecated
    protected User() {
    }

    public User(String username, @Nullable AuthProvider authProvider) {
        super(username);
        this.username = username;
        this.authProvider = authProvider;
        this.enabled = true;
    }

    @Override
    public LocationKind getKind() {
        return LocationKind.USER;
    }

    public @Nullable AuthProvider getAuthProvider() {
        return this.authProvider;
    }

    public void setUsername(String username) {
        this.username = username;
        // Keep Location.name aligned with username for polymorphic location search/display.
        this.setName(username);
    }

    public void setAuthProvider(@Nullable AuthProvider authProvider) {
        this.authProvider = authProvider;
        this.touchDateModified();
    }

    @Override
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
    protected Map<ResourceIdentifier, BuiltinPropertyBinding<Location, ?>> getBuiltinPropertyBindings() {
        return BUILTIN_PROPERTY_BINDINGS;
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
