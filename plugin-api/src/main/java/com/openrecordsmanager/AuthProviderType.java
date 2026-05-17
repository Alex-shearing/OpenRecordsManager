package com.openrecordsmanager;

public interface AuthProviderType {
    String getProviderName();
    boolean authenticate(String username, String password);
}
