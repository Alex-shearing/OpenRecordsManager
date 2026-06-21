package com.openrecordsmanager.model.repositories;

import com.openrecordsmanager.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuthProviderRepository extends JpaRepository<AuthProvider, UUID> {
}
