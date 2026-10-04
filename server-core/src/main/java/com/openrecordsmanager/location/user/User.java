package com.openrecordsmanager.location.user;

import com.google.common.base.Strings;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.location.Location;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.property.BuiltinProperty;
import com.openrecordsmanager.property.BuiltinPropertyBinding;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "user_details")
@DiscriminatorValue("USER")
@PrimaryKeyJoinColumn(name = "id")
@SuppressWarnings({"CanBeFinal"})
public class User extends Location implements UserDetails {
    public static final Map<ResourceIdentifier, BuiltinPropertyBinding<Location, ?>> BUILTIN_PROPERTY_BINDINGS =
            BuiltinPropertyBinding.scan(User.class);

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

    public User(String name, @Nullable AuthProvider authProvider, LocationType type) {
        super(name, type);
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

    /**
     * {@code "surname, given name"} when names are present, otherwise the stored location name.
     */
    @Override
    @Transient
    public String getDisplayName() {
        String surname = Strings.emptyToNull(this.surname);
        String givenName = Strings.emptyToNull(this.givenName);

        if (surname != null && givenName != null) {
            return surname + ", " + givenName;
        }
        if (surname != null || givenName != null) {
            return Objects.requireNonNullElse(surname, givenName);
        }
        return this.getName();
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

    /**
     * Spring Security login name; backed by {@link Location#getName()}.
     */
    @Override
    @Transient
    public String getUsername() {
        return this.getName();
    }
}
