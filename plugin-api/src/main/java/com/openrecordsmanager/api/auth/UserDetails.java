package com.openrecordsmanager.api.auth;

public record UserDetails(AuthProviderInstance provider, String username, String email) {
}
