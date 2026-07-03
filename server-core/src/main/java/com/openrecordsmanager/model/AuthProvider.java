package com.openrecordsmanager.model;

import com.openrecordsmanager.api.auth.AuthProviderInstance;
import com.openrecordsmanager.model.util.ResourceIdentifierConverter;
import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
}
