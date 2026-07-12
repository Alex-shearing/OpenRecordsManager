package com.openrecordsmanager.api.auth;

import java.security.Principal;

public record UserAuthDetails(AuthProviderInstance provider, String username, String email) implements Principal {
    @Override
    public String getName() {
        return this.username;
    }
}
