package com.openrecordsmanager.auth.entity;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.auth.*;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import com.openrecordsmanager.auth.AuthService;
import com.openrecordsmanager.auth.PluginAuthenticationProvider;
import com.openrecordsmanager.database.util.ComponentReferenceConverter;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "auth_provider")
@SuppressWarnings("NotNullFieldNotInitialized")
public class AuthProvider {

    @Id
    private UUID id;

    @Column(name = "provider_type", nullable = false)
    @Convert(converter = ComponentReferenceConverter.class)
    private ComponentReference<? extends AuthProviderType<?>> providerType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "settings", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, ?> settings = new HashMap<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Deprecated
    protected AuthProvider() {
    }

    public AuthProvider(
            ComponentCatalog catalog,
            String name,
            ComponentReference<? extends AuthProviderType<?>> providerType,
            Map<String, ?> settings
    ) {
        this.id = UuidVersion7Strategy.INSTANCE.generateUuid(null);
        this.name = name;
        this.providerType = providerType;
        this.setProperties(catalog, settings);
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, ?> getSettings(ComponentCatalog catalog) {
        AuthProviderType<?> t = this.getProviderType(catalog, AuthProviderType.class);
        return JsonSchemaValidator.serializeSettingsForClient(t.parseSettings(this.settings));
    }

    public void setProperties(ComponentCatalog catalog, Map<String, ?> properties) {
        AuthProviderType<?> type = this.getProviderType(catalog, AuthProviderType.class);
        Map<String, Object> merged = JsonSchemaValidator.mergeFromExisting(
                properties,
                this.settings
        );
        this.settings = JsonSchemaValidator.serializeSettings(type.parseSettings(merged));
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ComponentReference<? extends AuthProviderType<?>> getProviderType() {
        return this.providerType;
    }

    public <T extends AuthProviderType<?>> T getProviderType(ComponentCatalog catalog, Class<T> type) {
        AuthProviderType<?> genericProvider = this.providerType.getComponent(catalog)
                .orElseThrow(() -> new ResourceNotFoundException(this.providerType.getType(), this.providerType.getId(catalog).orElseThrow()));

        if (type.isInstance(genericProvider)) {
            @SuppressWarnings("unchecked")
            T value = (T) genericProvider;
            return value;
        }

        throw new ResourceNotFoundException(this.providerType.getType(), this.providerType.getId(catalog).orElseThrow());
    }

    public RedirectAuthChallenge beginRedirectLogin(ComponentCatalog catalog, String publicBaseUrl) {
        if (!this.isEnabled()) {
            throw new DisabledException("authentication provider is not enabled");
        }

        RedirectAuthProviderType<?> type = this.getProviderType(catalog, RedirectAuthProviderType.class);
        URI callbackUri = URI.create(publicBaseUrl + "/api/auth/callback/" + this.getId());
        return type.beginUntyped(callbackUri, this.settings);
    }

    public @Nullable UserAuthDetails login(ComponentCatalog catalog, AuthService authService, PluginAuthenticationProvider.AbstractPluginToken token) {
        if (!this.isEnabled()) {
            throw new DisabledException("authentication provider is not enabled");
        }

        AuthProviderType<?> type = this.getProviderType(catalog, AuthProviderType.class);

        return switch (type) {
            case InputAuthProviderType<?, ?> provider -> {
                PluginAuthenticationProvider.InputToken input = ((PluginAuthenticationProvider.InputToken) token);
                yield provider.authenticateUntyped(authService, this.settings, input.getCredentials());
            }
            case RedirectAuthProviderType<?> provider -> {
                PluginAuthenticationProvider.RedirectToken redirect = (PluginAuthenticationProvider.RedirectToken) token;
                yield provider.completeUntyped(
                        authService,
                        redirect.getCredentials(),
                        redirect.getPending(),
                        this.settings
                );
            }
            default -> throw new InternalAuthenticationServiceException("Unexpected provider type: " + type);
        };
    }
}
