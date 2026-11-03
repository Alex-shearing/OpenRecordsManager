package com.openrecordsmanager.template;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.property.ObjectProperty;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "managed_object_type")
@PrimaryKeyJoinColumn(name = "id")
public abstract class ManagedObjectType extends RegisteredComponent {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "base_type_id")
    @Nullable
    private ManagedObjectType baseType;

    @Column(name = "is_abstract", nullable = false)
    private boolean abstractType;

    @Deprecated
    protected ManagedObjectType() {
    }

    protected ManagedObjectType(ResourceIdentifier id) {
        super(id);
        this.abstractType = false;
    }

    protected ManagedObjectType(
            ResourceIdentifier id,
            boolean abstractType,
            @Nullable ManagedObjectType baseType
    ) {
        super(id);
        this.abstractType = abstractType;
        this.setBaseType(baseType);
    }

    /**
     * Concrete object kind, or {@code null} for an abstract location type that matches any location.
     */
    public abstract @Nullable ManagedObjectKind getKind();

    /**
     * Properties declared on this type only (not ancestors).
     */
    public abstract Set<ManagedObjectTypeProperty<?>> getDeclaredProperties();

    /**
     * Declared properties merged with ancestors; child overrides same property id.
     * Not {@code final}: Hibernate proxies must intercept this.
     */
    public List<ManagedObjectTypeProperty<?>> getEffectiveProperties() {
        LinkedHashMap<ResourceIdentifier, ManagedObjectTypeProperty<?>> merged = new LinkedHashMap<>();
        ManagedObjectType base = this.getBaseType();
        if (base != null) {
            for (ManagedObjectTypeProperty<?> binding : base.getEffectiveProperties()) {
                merged.put(binding.getProperty().getId(), binding);
            }
        }
        for (ManagedObjectTypeProperty<?> binding : this.getDeclaredProperties()) {
            merged.put(binding.getProperty().getId(), binding);
        }
        return List.copyOf(merged.values());
    }

    public final boolean hasProperty(ObjectProperty<?> property) {
        return this.getEffectiveProperties().stream()
                .anyMatch(binding -> Objects.equals(binding.getProperty(), property));
    }

    public final Set<ObjectProperty<?>> getObjectProperties() {
        return this.getEffectiveProperties().stream()
                .map(ManagedObjectTypeProperty::getProperty)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * API-facing check before constructing an instance of this type.
     */
    public final ManagedObjectKind getKindRequireInstantiable() {
        ManagedObjectKind kind = this.getKind();
        if (this.isAbstract() || kind == null) {
            throw ApiException.validationFailed(
                    "type",
                    "abstract_type_not_instantiable",
                    this.getId().toString()
            );
        }
        return kind;
    }

    /**
     * Ensures this type can be used to construct a managed object of {@code instanceKind}.
     * Rejects abstract types and kind mismatches.
     */
    public final void assertCompatibleWith(ManagedObjectKind instanceKind) {
        Objects.requireNonNull(instanceKind, "instanceKind");
        ManagedObjectKind typeKind = this.getKind();
        if (this.isAbstract() || typeKind != instanceKind) {
            throw new IllegalArgumentException(
                    "Type " + this.getId() + " kind " + typeKind
                            + " is not compatible with instance kind " + instanceKind
            );
        }
    }

    public boolean isAbstract() {
        return this.abstractType;
    }

    public @Nullable ManagedObjectType getBaseType() {
        return this.baseType;
    }

    public @Nullable ResourceIdentifier getBaseTypeId() {
        return this.baseType != null ? this.baseType.getId() : null;
    }

    public void setAbstractType(boolean abstractType) {
        this.abstractType = abstractType;
        this.touchDateModified();
    }

    public void setBaseType(@Nullable ManagedObjectType baseType) {
        if (baseType != null) {
            if (!compatibleKinds(this.getKind(), baseType.getKind())) {
                throw ApiException.validationFailed(
                        "baseTypeId",
                        "type_base_kind_mismatch",
                        String.valueOf(this.getKind()),
                        String.valueOf(baseType.getKind())
                );
            }
            ManagedObjectType cursor = baseType;
            while (cursor != null) {
                if (Objects.equals(cursor.getId(), this.getId())) {
                    throw ApiException.validationFailed("baseTypeId", "type_base_cycle");
                }
                cursor = cursor.getBaseType();
            }
        }
        this.baseType = baseType;
        this.touchDateModified();
    }

    /**
     * {@code null} kind means abstract any-location and is compatible with USER/GROUP/null only.
     */
    static boolean compatibleKinds(
            @Nullable ManagedObjectKind self,
            @Nullable ManagedObjectKind base
    ) {
        if (self == null || base == null) {
            return (self == null || self.isLocation()) && (base == null || base.isLocation());
        }
        return self == base;
    }

    /**
     * True if this type or any ancestor equals {@code constraint}.
     */
    public boolean isAssignableTo(ManagedObjectType constraint) {
        ManagedObjectType cursor = this;
        while (cursor != null) {
            if (Objects.equals(cursor.getId(), constraint.getId())) {
                return true;
            }
            cursor = cursor.getBaseType();
        }
        return false;
    }
}
