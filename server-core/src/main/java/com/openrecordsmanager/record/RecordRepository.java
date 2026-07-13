package com.openrecordsmanager.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordRepository extends JpaRepository<Record, UUID> {
    @Query("SELECT e FROM RecordRevision e WHERE e.record.id = :recordId AND e.version = :version")
    Optional<RecordRevision> findByRecordId(@Param("recordId") UUID recordId, @Param("version") String version);
}
