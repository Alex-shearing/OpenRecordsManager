package com.openrecordsmanager.model;

import com.openrecordsmanager.auth.AuthProviderInstance;
import com.openrecordsmanager.model.util.ResourceIdentifierDbConverter;
import com.openrecordsmanager.resources.ResourceIdentifier;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "auth_provider")
public class AuthProvider implements AuthProviderInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_type", nullable = false)
    @Convert(converter = ResourceIdentifierDbConverter.class)
    private ResourceIdentifier providerType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "settings", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> settings;

    public ResourceIdentifier getProviderType() {
        return this.providerType;
    }

    @Override
    public Long getId() {
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
