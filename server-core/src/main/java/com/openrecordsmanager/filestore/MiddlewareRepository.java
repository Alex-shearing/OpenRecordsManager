package com.openrecordsmanager.filestore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MiddlewareRepository extends JpaRepository<Middleware<?>, UUID> {
}
