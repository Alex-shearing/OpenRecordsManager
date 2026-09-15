package com.openrecordsmanager.filestore.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileStoreRepository extends JpaRepository<FileStore, UUID> {
    boolean existsByMiddlewares(UUID middlewareId);

    @Query("SELECT COUNT(e) FROM FileStoreEntry e WHERE e.store.id = :storeId")
    long countFilesByStoreId(@Param("storeId") UUID storeId);
}
