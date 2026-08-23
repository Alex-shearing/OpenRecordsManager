package com.openrecordsmanager.auth.entity;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.auth.AuthProviderInstance;
import com.openrecordsmanager.api.auth.AuthProviderType;
import com.openrecordsmanager.database.util.ComponentReferenceConverter;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "auth_provider")
@SuppressWarnings("NotNullFieldNotInitialized")
public class AuthProvider implements AuthProviderInstance {

    @Id
    private UUID id;

    @Column(name = "provider_type", nullable = false)
    @Convert(converter = ComponentReferenceConverter.class)
    private ComponentReference<? extends AuthProviderType> providerType;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "settings", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> settings = new HashMap<>();

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Map<String, Object> getSettings() {
        return this.settings;
    }

    @Deprecated
    protected AuthProvider() {
    }

    public AuthProvider(String name, ComponentReference<? extends AuthProviderType> providerType, Map<String, Object> settings) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.providerType = providerType;
        this.settings = settings;
    }

    public ComponentReference<? extends AuthProviderType> getProviderType() {
        return providerType;
    }

    @SuppressWarnings("unchecked")
    public <T extends AuthProviderType> T getProviderType(ComponentCatalog catalog, Class<T> type) {
        AuthProviderType genericProvider = this.providerType.getComponent(catalog)
                .orElseThrow(() -> new ResourceNotFoundException(this.providerType.getType(), this.providerType.getId(catalog).orElseThrow()));

        if (type.isInstance(genericProvider)) {
            return (T) genericProvider;
        }

        throw new ResourceNotFoundException(this.providerType.getType(), this.providerType.getId(catalog).orElseThrow());
    }
}
