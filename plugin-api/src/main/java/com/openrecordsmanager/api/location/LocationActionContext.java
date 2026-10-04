package com.openrecordsmanager.api.location;

import com.openrecordsmanager.api.audit.AuditEmitter;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface LocationActionContext {
    UUID getActorId();

    String getActorUsername();

    UUID getTargetLocationId();

    LocationKind getTargetKind();

    /**
     * Login/storage name when the target is a user location; empty otherwise.
     */
    Optional<String> getTargetUsername();

    String getTargetName();

    ConfigStore getConfig();

    <T> boolean isPropertyRegistered(ObjectPropertyTemplate<T> property);

    <T> Optional<T> getTargetProperty(ObjectPropertyTemplate<T> property);

    <T> void setTargetProperty(ObjectPropertyTemplate<T> property, @Nullable T value);

    AuditEmitter getAudit();
}
