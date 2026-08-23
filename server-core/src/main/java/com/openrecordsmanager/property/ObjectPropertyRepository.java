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
            SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END
            FROM RecordPropertyValue v
            WHERE v.property = :property
            """)
    boolean isUsedByRecords(@Param("property") ObjectProperty<?> property);

    @Query("""
            SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END
            FROM UserPropertyValue v
            WHERE v.property = :property
            """)
    boolean isUsedByUsers(@Param("property") ObjectProperty<?> property);
}
