package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.resources.ResourceIdentifier;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.runtime.CelFunctionBinding;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.util.*;

@Entity
@Table(name = "user")
public class User {

    @Id
    @JsonProperty
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_provider_id")
    @Nullable
    public AuthProvider authProvider;

    @OneToMany(cascade = CascadeType.ALL)
    @JsonProperty
    public Set<UserPropertyValue<?>> properties;

    public User() {
        this.id = UUID.randomUUID();
        this.properties = new HashSet<>();
    }

    public User(@Nullable AuthProvider provider, Set<UserPropertyValue<?>> properties) {
        this.id = UUID.randomUUID();
        this.authProvider = provider;
        this.properties = properties;
    }

    public <T> T getProperty(ObjectProperty<T> userProperty) {
        return (T) this.getProperty(userProperty.id.getId());
    }

    public Object getProperty(ResourceIdentifier id) {
        Optional<UserPropertyValue<?>> property = this.properties.stream().filter(recordPropertyValue -> Objects.equals(recordPropertyValue.property.id.getId(), id)).findFirst();
        return property.map(userPropertyValue -> userPropertyValue.value).orElse(null);
    }

    public static CelFunctionDecl getCompilerDeclaration() {
        return CelFunctionDecl.newFunctionDeclaration(
                "_[_]",
                CelOverloadDecl.newGlobalOverload(
                        "custom_index_map",  // Unique overload ID
                        SimpleType.ANY,             // Result type
                        SimpleType.ANY,             // Argument 1 (the container map)
                        SimpleType.STRING           // Argument 2 (the key)
                )
        );
    }

    public static CelFunctionBinding getRuntimeBinding() {
        return CelFunctionBinding.from(
                "custom_index_map", // The name used in CEL scripts
                User.class,
                String.class, // Argument type
                (user, arg) -> user.getProperty(ResourceIdentifier.valueOf(arg))
        );
    }
}
