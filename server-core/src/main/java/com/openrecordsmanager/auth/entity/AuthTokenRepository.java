package com.openrecordsmanager.auth.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    void deleteByUser_Id(UUID userId);

    void deleteByUser_AuthProvider_Id(UUID authProviderId);
}
