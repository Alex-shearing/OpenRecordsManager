package com.openrecordsmanager.auth.entity;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.auth.*;
import com.openrecordsmanager.auth.AuthService;
import com.openrecordsmanager.auth.PluginAuthenticationProvider;
import com.openrecordsmanager.database.util.ComponentReferenceConverter;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import com.openrecordsmanager.schema.JsonSchemaValidator;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

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

    @Column(nullable = false)
    private Instant dateCreated;

    @Column(nullable = false)
    private Instant dateModified;

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
        this.dateCreated = Instant.now();
        this.dateModified = Instant.now();
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

    public Map<String, ?> getSettings(ComponentCatalog catalog) {
        return withParsedSettings(
                this.getProviderType(catalog),
                this.settings,
                (_, s) -> JsonSchemaValidator.serializeSettingsForClient(s)
        );
    }

    public void setProperties(ComponentCatalog catalog, Map<String, ?> properties) {
        this.settings = withParsedSettings(
                this.getProviderType(catalog),
                JsonSchemaValidator.mergeFromExisting(properties, this.settings),
                (_, s) -> JsonSchemaValidator.serializeSettings(s)
        );
        this.touchDateModified();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.touchDateModified();
    }

    public ComponentReference<? extends AuthProviderType<?>> getProviderType() {
        return this.providerType;
    }

    public AuthProviderType<?> getProviderType(ComponentCatalog catalog) {
        return this.providerType.getComponent(catalog)
                .orElseThrow(() -> new ResourceNotFoundException(
                        this.providerType.getType(),
                        this.providerType.getId(catalog).orElseThrow()
                ));
    }

    public RedirectAuthChallenge beginRedirectLogin(ComponentCatalog catalog, String publicBaseUrl) {
        if (!this.isEnabled()) {
            throw new DisabledException("authentication provider is not enabled");
        }

        if (!(this.getProviderType(catalog) instanceof RedirectAuthProviderType<?> type)) {
            throw new ResourceNotFoundException(
                    this.providerType.getType(),
                    this.providerType.getId(catalog).orElseThrow()
            );
        }

        URI callbackUri = URI.create(publicBaseUrl + "/api/auth/callback/" + this.getId());
        return withParsedSettings(type, this.settings, (t, s) -> t.begin(callbackUri, s));
    }

    public @Nullable UserAuthDetails login(ComponentCatalog catalog, AuthService authService, PluginAuthenticationProvider.AbstractPluginToken token) {
        if (!this.isEnabled()) {
            throw new DisabledException("authentication provider is not enabled");
        }

        AuthProviderType<?> type = this.getProviderType(catalog);
        return switch (type) {
            case InputAuthProviderType<?, ?> provider -> {
                PluginAuthenticationProvider.InputToken input = ((PluginAuthenticationProvider.InputToken) token);
                yield withParsedSettings(provider, this.settings, (p, s) -> p.authenticate(
                        authService,
                        s,
                        JsonSchemaValidator.toRecord(p.getInputClass(), input.getCredentials())
                ));
            }
            case RedirectAuthProviderType<?> provider -> {
                PluginAuthenticationProvider.RedirectToken redirect = (PluginAuthenticationProvider.RedirectToken) token;
                yield withParsedSettings(provider, this.settings, (p, s) -> p.complete(
                        authService,
                        redirect.getCredentials(),
                        redirect.getPending(),
                        s
                ));
            }
            default -> throw new InternalAuthenticationServiceException("Unexpected provider type: " + type);
        };
    }

    private static <R extends @Nullable Object, S extends Record, T extends AuthProviderType<S>> R withParsedSettings(
            T type,
            Map<String, ?> settings,
            BiFunction<T, S, R> function
    ) {
        return function.apply(type, JsonSchemaValidator.toRecord(type.getSettingsClass(), settings));
    }
}
