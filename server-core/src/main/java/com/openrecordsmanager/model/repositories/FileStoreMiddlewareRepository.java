package com.openrecordsmanager.model.repositories;

import com.openrecordsmanager.model.FileStoreMiddleware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileStoreMiddlewareRepository extends JpaRepository<FileStoreMiddleware<?>, UUID> {
    @Query("SELECT s.id FROM FileStoreMiddleware s")
    UUID[] findAllIds();
}
