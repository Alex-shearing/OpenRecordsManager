package com.openrecordsmanager.auth;

import com.openrecordsmanager.auth.entity.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthProviderRepository extends JpaRepository<AuthProvider, UUID> {
    Optional<AuthProvider> findByIdAndEnabledTrue(UUID id);

    List<AuthProvider> findByEnabledTrue();
}
