package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ObjectPropertyRepository extends JpaRepository<ObjectProperty<?>, ResourceIdentifier> {

    @Query("""
            SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END
            FROM RecordType rt JOIN rt.properties p
            WHERE p.property = :property
            """)
    boolean isAssignedToRecordType(@Param("property") ObjectProperty<?> property);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Record r JOIN r.properties v
            WHERE KEY(v) = :property
            """)
    boolean isUsedByRecords(@Param("property") ObjectProperty<?> property);

    @Query("""
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
            FROM User u JOIN u.properties v
            WHERE KEY(v) = :property
            """)
    boolean isUsedByUsers(@Param("property") ObjectProperty<?> property);
}
