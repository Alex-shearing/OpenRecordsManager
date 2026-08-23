package com.openrecordsmanager.list;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ListTypeRepository extends JpaRepository<ListType, ResourceIdentifier> {

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM ObjectProperty p
            WHERE p.listType = :listType
            """)
    boolean isUsedByObjectProperties(@Param("listType") ListType listType);
}
