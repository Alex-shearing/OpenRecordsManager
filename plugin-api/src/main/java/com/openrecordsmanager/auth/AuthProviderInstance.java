package com.openrecordsmanager.auth;

import java.util.Map;

public interface AuthProviderInstance {
    Long getId();

    String getName();

    Map<String, Object> getSettings();
}
