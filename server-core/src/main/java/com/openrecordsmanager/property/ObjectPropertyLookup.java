package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves persisted {@link ObjectProperty} entities by id for use from domain objects
 * (same install pattern as {@link PropertyValueCodec}).
 */
@Component
public class ObjectPropertyLookup {

    private static volatile @Nullable ObjectPropertyLookup installed;

    private final ObjectPropertyRepository repository;
    private final ConcurrentHashMap<ResourceIdentifier, ObjectProperty<?>> cache = new ConcurrentHashMap<>();

    public ObjectPropertyLookup(ObjectPropertyRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void install() {
        installed = this;
    }

    @PreDestroy
    void uninstall() {
        if (installed == this) {
            installed = null;
        }
    }

    public static ObjectPropertyLookup requireInstalled() {
        ObjectPropertyLookup lookup = installed;
        if (lookup == null) {
            throw new IllegalStateException("ObjectPropertyLookup is not installed");
        }
        return lookup;
    }

    public Optional<ObjectProperty<?>> find(ResourceIdentifier id) {
        ObjectProperty<?> cached = this.cache.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        return this.repository.findById(id)
                .map(property -> {
                    this.cache.putIfAbsent(id, property);
                    return property;
                });
    }

    public ObjectProperty<?> require(ResourceIdentifier id) {
        return find(id).orElseThrow(() -> new IllegalStateException("Missing object property: " + id));
    }

}
