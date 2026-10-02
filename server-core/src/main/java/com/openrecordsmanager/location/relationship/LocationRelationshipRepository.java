package com.openrecordsmanager.location.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRelationshipRepository extends JpaRepository<LocationRelationship, UUID> {
    @Query("""
            SELECT r FROM LocationRelationship r
            WHERE r.source.id = :locationId
              AND r.activeTo IS NULL
              AND (:typeId IS NULL OR r.type.id = :typeId)
            """)
    List<LocationRelationship> findActiveOutgoing(
            @Param("locationId") UUID locationId,
            @Param("typeId") @Nullable ResourceIdentifier typeId
    );

    @Query("""
            SELECT r FROM LocationRelationship r
            WHERE r.target.id = :locationId
              AND r.activeTo IS NULL
              AND (:typeId IS NULL OR r.type.id = :typeId)
            """)
    List<LocationRelationship> findActiveIncoming(
            @Param("locationId") UUID locationId,
            @Param("typeId") @Nullable ResourceIdentifier typeId
    );

    @Query("""
            SELECT r FROM LocationRelationship r
            WHERE r.source.id = :sourceId
              AND r.target.id = :targetId
              AND r.type.id = :typeId
              AND r.activeTo IS NULL
            """)
    Optional<LocationRelationship> findActiveEdge(
            @Param("sourceId") UUID sourceId,
            @Param("targetId") UUID targetId,
            @Param("typeId") ResourceIdentifier typeId
    );

    @Query("""
            SELECT r FROM LocationRelationship r
            WHERE r.source.id = :sourceId
              AND r.type.id = :typeId
              AND r.activeTo IS NULL
            """)
    List<LocationRelationship> findActiveBySourceAndType(
            @Param("sourceId") UUID sourceId,
            @Param("typeId") ResourceIdentifier typeId
    );
}
