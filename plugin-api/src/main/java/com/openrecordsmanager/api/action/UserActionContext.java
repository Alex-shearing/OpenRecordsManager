package com.openrecordsmanager.api.action;

import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public interface UserActionContext {
    UUID getActorId();

    String getActorUsername();

    UUID getTargetUserId();

    String getTargetUsername();

    ConfigStore getConfig();

    <T> boolean isPropertyRegistered(ObjectPropertyTemplate<T> property);

    <T> Optional<T> getTargetProperty(ObjectPropertyTemplate<T> property);

    <T> void setTargetProperty(ObjectPropertyTemplate<T> property, @Nullable T value);
}
