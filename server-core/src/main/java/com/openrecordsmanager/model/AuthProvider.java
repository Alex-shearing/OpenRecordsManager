package com.openrecordsmanager.model;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.auth.AuthProviderInstance;
import com.openrecordsmanager.model.util.ResourceIdentifierConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "auth_provider")
public class AuthProvider implements AuthProviderInstance {

    @Id
    private UUID id;

    @Column(name = "provider_type", nullable = false)
    @Convert(converter = ResourceIdentifierConverter.class)
    public ResourceIdentifier providerType;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "settings", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> settings;

    @Override
    public @NonNull UUID getId() {
        return this.id;
    }

    @Override
    public @NonNull String getName() {
        return this.name;
    }

    @Override
    public @NonNull Map<String, Object> getSettings() {
        return this.settings;
    }
}
