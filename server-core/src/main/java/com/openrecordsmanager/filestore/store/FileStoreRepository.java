package com.openrecordsmanager.filestore.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileStoreRepository extends JpaRepository<FileStore, UUID> {
    boolean existsByMiddlewares(UUID middlewareId);
}
