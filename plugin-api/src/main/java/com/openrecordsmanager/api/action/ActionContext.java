package com.openrecordsmanager.api.action;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.audit.AuditEmitter;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Runtime context for a location or record action: actor, target, config, properties, audit.
 */
public interface ActionContext {
    UUID getActorId();

    String getActorUsername();

    UUID getTargetId();

    ManagedObjectKind getTargetKind();

    ConfigStore getConfig();

    <T> boolean isPropertyRegistered(ComponentReference<ObjectPropertyTemplate<T>> property);

    <T> Optional<T> getTargetProperty(ComponentReference<ObjectPropertyTemplate<T>> property);

    <T> void setTargetProperty(ComponentReference<ObjectPropertyTemplate<T>> property, @Nullable T value);

    AuditEmitter getAudit();
}
