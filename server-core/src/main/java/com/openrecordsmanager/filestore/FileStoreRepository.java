package com.openrecordsmanager.filestore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileStoreRepository extends JpaRepository<FileStore<?>, UUID> {
    @Query("SELECT s.id FROM FileStore s")
    UUID[] findAllIds();
}
