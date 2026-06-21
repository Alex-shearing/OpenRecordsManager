package com.openrecordsmanager.auth;

import java.util.Map;
import java.util.UUID;

public interface AuthProviderInstance {
    UUID getId();

    String getName();

    Map<String, Object> getSettings();
}
