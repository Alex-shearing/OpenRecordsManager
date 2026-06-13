package com.openrecordsmanager.auth;

public record UserDetails(AuthProviderInstance provider, String username, String email) {
}
