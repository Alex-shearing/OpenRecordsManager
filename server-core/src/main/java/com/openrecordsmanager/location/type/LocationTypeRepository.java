package com.openrecordsmanager.location.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationTypeRepository extends JpaRepository<LocationType, ResourceIdentifier> {
    @Query("SELECT t.id FROM LocationType t")
    List<ResourceIdentifier> findAllIds();
}
