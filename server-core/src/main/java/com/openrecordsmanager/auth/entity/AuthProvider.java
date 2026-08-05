package com.openrecordsmanager.auth.entity;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.auth.AuthProviderInstance;
import com.openrecordsmanager.api.auth.AuthProviderType;
import com.openrecordsmanager.database.util.ComponentReferenceConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "auth_provider")
public class AuthProvider implements AuthProviderInstance {

    @Id
    public UUID id;

    @Column(name = "provider_type", nullable = false)
    @Convert(converter = ComponentReferenceConverter.class)
    public ComponentReference<? extends AuthProviderType> providerType;

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
