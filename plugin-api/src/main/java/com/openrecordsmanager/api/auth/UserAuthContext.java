package com.openrecordsmanager.api.auth;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;

import java.util.Optional;

public interface UserAuthContext {
    <T> Optional<T> getUserProperty(String username, ComponentReference<ObjectPropertyTemplate<T>> property);
}
