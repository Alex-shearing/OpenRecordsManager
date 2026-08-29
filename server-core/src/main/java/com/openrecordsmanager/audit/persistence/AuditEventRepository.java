package com.openrecordsmanager.audit.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    @Query("""
            SELECT e FROM AuditEventEntity e
            WHERE e.targetType = :targetType AND e.targetId = :targetId
            AND e.occurredAt < :before
            ORDER BY e.occurredAt DESC
            """)
    List<AuditEventEntity> findByTargetBefore(
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("before") Instant before,
            Pageable pageable
    );

    List<AuditEventEntity> findByTargetTypeAndTargetIdOrderByOccurredAtDesc(
            String targetType,
            String targetId,
            Pageable pageable
    );
}
